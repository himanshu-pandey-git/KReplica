package io.availe.builders

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.availe.helpers.*
import io.availe.models.*

private fun parseApplyAnnotations(
    declaration: KSClassDeclaration,
    masterDtoVariants: Set<DtoVariant>,
    environment: SymbolProcessorEnvironment
): List<AnnotationConfigModel> {
    val applyAnnotations = declaration.annotations.filter {
        it.isAnnotation(REPLICATE_APPLY_ANNOTATION_NAME)
    }

    return applyAnnotations.flatMap { annotation ->
        val includeArg = (annotation.arguments.first { it.name?.asString() == "include" }.value as List<*>)
            .map { DtoVariant.valueOf((it as KSDeclaration).simpleName.asString()) }.toSet()
        val excludeArg = (annotation.arguments.first { it.name?.asString() == "exclude" }.value as List<*>)
            .map { DtoVariant.valueOf((it as KSDeclaration).simpleName.asString()) }.toSet()
        val annotationsToApply = annotation.arguments.first { it.name?.asString() == "annotations" }.value as List<*>

        val allTargetedVariants = includeArg + excludeArg
        val unknownVariants = allTargetedVariants - masterDtoVariants
        if (unknownVariants.isNotEmpty()) {
            fail(
                environment,
                "KReplica Validation Error in '${declaration.simpleName.asString()}': " +
                        "@ApplyAnnotations targets unknown variants: ${unknownVariants.joinToString()}. " +
                        "Allowed variants are: [${masterDtoVariants.joinToString()}]."
            )
        }

        val initialSet = if (includeArg.isNotEmpty()) includeArg else masterDtoVariants
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
    val modelDtoVariants = if (modelVariantsArgument == null) {
        fail(
            environment,
            "KReplica Error: The 'variants' argument is mandatory on @Replicate.Model for model '${declaration.simpleName.asString()}'. " +
                    "Please explicitly specify which variants to generate, e.g., @Replicate.Model(variants = [Variant.BASE])."
        )
    } else {
        (modelVariantsArgument.value as List<*>).map {
            DtoVariant.valueOf((it as KSDeclaration).simpleName.asString())
        }.toSet()
    }

    val modelNominalTyping = modelAnnotation.arguments
        .find { it.name?.asString() == "nominalTyping" }
        ?.let { NominalTyping.valueOf((it.value as KSDeclaration).simpleName.asString()) }
        ?: NominalTyping.DISABLED

    if (modelNominalTyping == NominalTyping.INHERIT) {
        fail(
            environment,
            "Model '${declaration.simpleName.asString()}' cannot use nominalTyping = INHERIT. " +
                    "Please specify ENABLED or DISABLED."
        )
    }

    val modelAutoContextual = modelAnnotation.arguments
        .find { it.name?.asString() == "autoContextual" }
        ?.let { AutoContextual.valueOf((it.value as KSDeclaration).simpleName.asString()) }
        ?: AutoContextual.ENABLED

    if (modelAutoContextual == AutoContextual.INHERIT) {
        fail(
            environment,
            "Model '${declaration.simpleName.asString()}' cannot use autoContextual = INHERIT. " +
                    "Please specify ENABLED or DISABLED."
        )
    }

    val annotationConfigs = parseApplyAnnotations(declaration, modelDtoVariants, environment)
    val modelAnnotations = declaration.annotations.toAnnotationModels(frameworkDeclarations)

    val versioningInfo = determineVersioningInfo(declaration, environment)
    val properties = declaration.getAllProperties().map { property ->
        processProperty(property, modelDtoVariants, modelNominalTyping, modelAutoContextual, resolver, frameworkDeclarations, environment)
    }.toMutableList()

    if (versioningInfo != null && properties.none { it.name == SCHEMA_VERSION_FIELD }) {
        val schemaVersionProperty = RegularProperty(
            name = SCHEMA_VERSION_FIELD,
            typeInfo = TypeInfo("kotlin.Int", isNullable = false),
            dtoVariants = modelDtoVariants,
            annotations = null,
            nominalTyping = modelNominalTyping,
            autoContextual = modelAutoContextual
        )
        properties.add(schemaVersionProperty)
    }

    val allOptInMarkers = extractAllOptInMarkers(declaration).takeIf { it.isNotEmpty() }

    return Model(
        name = declaration.simpleName.asString(),
        packageName = declaration.packageName.asString(),
        properties = properties,
        dtoVariants = modelDtoVariants,
        annotationConfigs = annotationConfigs,
        annotations = modelAnnotations,
        optInMarkers = allOptInMarkers,
        isVersionOf = versioningInfo?.baseModelName,
        schemaVersion = versioningInfo?.schemaVersion,
        nominalTyping = modelNominalTyping,
        autoContextual = modelAutoContextual
    )
}