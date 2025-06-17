package io.availe.builders

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.availe.NominalTyping
import io.availe.helpers.*
import io.availe.models.*

private fun validateSuperclassForSerialization(
    modelName: String,
    superclassType: KSType,
    environment: SymbolProcessorEnvironment
) {
    val superclassDecl = superclassType.declaration as? KSClassDeclaration ?: return
    val isSuperclassSerializable = superclassDecl.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlinx.serialization.Serializable"
    }
    if (isSuperclassSerializable) return

    val hasNoArgConstructor = superclassDecl.primaryConstructor?.parameters?.isEmpty() ?: true
    if (hasNoArgConstructor) return

    fail(
        environment,
        """
        KReplica Validation Error in model '$modelName':
        This model is marked as @Serializable and configured to extend the non-serializable class '${superclassDecl.qualifiedName?.asString()}'.
        This is not supported by the kotlinx.serialization plugin.

        To fix this, you must do one of the following:
          1. Add @Serializable to the '${superclassDecl.simpleName.asString()}' class.
          2. Ensure '${superclassDecl.simpleName.asString()}' has a zero-argument primary constructor.
        """.trimIndent()
    )
}

private fun parseExtendVariantAnnotations(
    declaration: KSClassDeclaration,
    masterVariants: Set<Variant>,
    environment: SymbolProcessorEnvironment
): List<ExtendConfigModel> {
    val extendAnnotations = declaration.annotations.filter {
        it.isAnnotation(EXTEND_VARIANT_ANNOTATION_NAME)
    }

    val targets = extendAnnotations.map { annotation ->
        val variantArgument = annotation.arguments.first { it.name?.asString() == "variant" }
        (variantArgument.value as KSDeclaration).simpleName.asString()
    }.toList()

    val duplicateTargets = targets.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
    if (duplicateTargets.isNotEmpty()) {
        fail(
            environment,
            "KReplica Validation Error in '${declaration.simpleName.asString()}': " +
                    "The following variants are targeted by more than one @ExtendVariant annotation, which is illegal: ${duplicateTargets.joinToString()}."
        )
    }

    return extendAnnotations.map { annotation ->
        val variantArgument = annotation.arguments.first { it.name?.asString() == "variant" }
        val variantEnumName = (variantArgument.value as KSDeclaration).simpleName.asString()
        val variant = Variant.valueOf(variantEnumName)

        if (variant !in masterVariants) {
            fail(
                environment,
                "KReplica Validation Error in '${declaration.simpleName.asString()}': " +
                        "@ExtendVariant targets variant '$variant', which is not declared in the @Replicate 'variants' list: [${masterVariants.joinToString()}]."
            )
        }

        val typeArgument = annotation.arguments.first { it.name?.asString() == "with" }.value as KSType
        val superclassFqName = typeArgument.declaration.qualifiedName!!.asString()
        val superclassDecl = typeArgument.declaration as KSClassDeclaration
        val sourcePropertyNames = declaration.getAllProperties().map { it.simpleName.asString() }.toSet()
        val superclassPropertyNames = superclassDecl.getAllProperties().map { it.simpleName.asString() }.toSet()
        val undeclaredProperties = superclassPropertyNames - sourcePropertyNames
        if (undeclaredProperties.isNotEmpty()) {
            fail(
                environment,
                """
                KReplica Validation Error in '${declaration.simpleName.asString()}':
                The superclass '${superclassDecl.qualifiedName?.asString()}' used in @ExtendVariant introduces properties that are not declared in the source interface: [${undeclaredProperties.joinToString()}].
                To ensure API visibility and prevent breaking changes, all replicated properties must be declared on the source interface.
                """.trimIndent()
            )
        }


        ExtendConfigModel(
            superclassFqName = superclassFqName,
            variant = variant
        )
    }.toList()
}

private fun parseApplyAnnotations(
    declaration: KSClassDeclaration,
    masterVariants: Set<Variant>,
    environment: SymbolProcessorEnvironment
): List<AnnotationConfigModel> {
    val applyAnnotations = declaration.annotations.filter {
        it.isAnnotation(APPLY_ANNOTATIONS_ANNOTATION_NAME)
    }

    return applyAnnotations.flatMap { annotation ->
        val includeArg = (annotation.arguments.first { it.name?.asString() == "include" }.value as List<*>)
            .map { Variant.valueOf((it as KSDeclaration).simpleName.asString()) }.toSet()
        val excludeArg = (annotation.arguments.first { it.name?.asString() == "exclude" }.value as List<*>)
            .map { Variant.valueOf((it as KSDeclaration).simpleName.asString()) }.toSet()
        val annotationsToApply = annotation.arguments.first { it.name?.asString() == "annotations" }.value as List<*>

        val allTargetedVariants = includeArg + excludeArg
        val unknownVariants = allTargetedVariants - masterVariants
        if (unknownVariants.isNotEmpty()) {
            fail(
                environment,
                "KReplica Validation Error in '${declaration.simpleName.asString()}': " +
                        "@ApplyAnnotations targets unknown variants: ${unknownVariants.joinToString()}. " +
                        "Allowed variants are: [${masterVariants.joinToString()}]."
            )
        }

        val initialSet = if (includeArg.isNotEmpty()) includeArg else masterVariants
        val finalVariants = initialSet - excludeArg

        annotationsToApply.map { annotationToApplyType ->
            val annotationFqName = (annotationToApplyType as KSType).declaration.qualifiedName!!.asString()
            AnnotationConfigModel(
                annotation = AnnotationModel(qualifiedName = annotationFqName),
                variants = finalVariants
            )
        }
    }.toList()
}

internal fun buildModel(
    declaration: KSClassDeclaration,
    resolver: Resolver,
    frameworkDeclarations: Set<KSClassDeclaration>,
    environment: SymbolProcessorEnvironment
): Model {
    val modelAnnotation = declaration.annotations.first { it.isAnnotation(MODEL_ANNOTATION_NAME) }

    val modelVariantsArgument = modelAnnotation.arguments.find { it.name?.asString() == "variants" }
    val modelVariants = if (modelVariantsArgument == null) {
        setOf(Variant.BASE, Variant.CREATE, Variant.PATCH)
    } else {
        (modelVariantsArgument.value as List<*>).map {
            Variant.valueOf((it as KSDeclaration).simpleName.asString())
        }.toSet()
    }

    val modelNominalTyping = modelAnnotation.arguments
        .find { it.name?.asString() == "nominalTyping" }
        ?.let { (it.value as KSDeclaration).simpleName.asString() }
        ?: NominalTyping.DISABLED.name

    if (modelNominalTyping == "INHERIT") {
        fail(
            environment,
            "Model '${declaration.simpleName.asString()}' cannot use nominalTyping = INHERIT. " +
                    "Please specify ENABLED or DISABLED."
        )
    }

    val extendConfigs = parseExtendVariantAnnotations(declaration, modelVariants, environment)
    val annotationConfigs = parseApplyAnnotations(declaration, modelVariants, environment)

    extendConfigs.forEach { config ->
        val isSerializable = annotationConfigs.any { ac ->
            config.variant in ac.variants && ac.annotation.qualifiedName == "kotlinx.serialization.Serializable"
        }
        if (isSerializable) {
            val superclassType =
                resolver.getClassDeclarationByName(resolver.getKSNameFromString(config.superclassFqName))
                    ?.asType(emptyList())
            if (superclassType != null) {
                validateSuperclassForSerialization(declaration.simpleName.asString(), superclassType, environment)
            }
        }
    }

    val versioningInfo = determineVersioningInfo(declaration, environment)
    val properties = declaration.getAllProperties().map { property ->
        processProperty(property, modelVariants, modelNominalTyping, resolver, frameworkDeclarations, environment)
    }.toMutableList()

    if (versioningInfo != null && properties.none { it.name == SCHEMA_VERSION_FIELD }) {
        val schemaVersionProperty = RegularProperty(
            name = SCHEMA_VERSION_FIELD,
            typeInfo = TypeInfo("kotlin.Int", isNullable = false),
            variants = modelVariants
        )
        properties.add(schemaVersionProperty)
    }

    val allOptInMarkers = extractAllOptInMarkers(declaration).takeIf { it.isNotEmpty() }

    return Model(
        name = declaration.simpleName.asString(),
        packageName = declaration.packageName.asString(),
        properties = properties,
        variants = modelVariants,
        annotationConfigs = annotationConfigs,
        optInMarkers = allOptInMarkers,
        isVersionOf = versioningInfo?.baseModelName,
        schemaVersion = versioningInfo?.schemaVersion,
        nominalTyping = modelNominalTyping,
        extendConfigs = extendConfigs
    )
}