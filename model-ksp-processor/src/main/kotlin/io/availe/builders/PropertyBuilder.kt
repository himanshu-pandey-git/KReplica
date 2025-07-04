package io.availe.builders

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import io.availe.helpers.*
import io.availe.models.*

private fun isGeneratedVariantContainer(declaration: KSClassDeclaration?): Boolean {
    if (declaration == null || Modifier.SEALED !in declaration.modifiers) return false
    val nestedDecls = declaration.declarations
        .filterIsInstance<KSClassDeclaration>()
        .map { it.simpleName.asString() }
        .toSet()
    return DtoVariant.entries.all { it.suffix in nestedDecls }
}

internal fun processProperty(
    propertyDeclaration: KSPropertyDeclaration,
    modelDtoVariants: Set<DtoVariant>,
    modelNominalTyping: String?,
    resolver: Resolver,
    frameworkDeclarations: Set<KSClassDeclaration>,
    environment: SymbolProcessorEnvironment
): Property {
    if (propertyDeclaration.isMutable) {
        val propertyName = propertyDeclaration.simpleName.asString()
        val interfaceName = (propertyDeclaration.parent as? KSClassDeclaration)?.simpleName?.asString() ?: "Unknown"
        fail(
            environment,
            """
            KReplica Validation Error: Property '$propertyName' in interface '$interfaceName' is declared as 'var'.
            Source model interfaces for KReplica must use immutable properties ('val').
            Please change '$propertyName' from 'var' to 'val'.
            """.trimIndent()
        )
    }

    val fieldAnnotation =
        propertyDeclaration.annotations.firstOrNull { it.isAnnotation(REPLICATE_PROPERTY_ANNOTATION_NAME) }

    val propertyVariants = if (fieldAnnotation == null) {
        modelDtoVariants
    } else {
        val includeArg = fieldAnnotation.arguments.find { it.name?.asString() == "include" }?.value as? List<*>
        val excludeArg = fieldAnnotation.arguments.find { it.name?.asString() == "exclude" }?.value as? List<*>

        val include = includeArg?.map { DtoVariant.valueOf(it.toString().substringAfterLast('.')) }?.toSet() ?: emptySet()
        val exclude = excludeArg?.map { DtoVariant.valueOf(it.toString().substringAfterLast('.')) }?.toSet() ?: emptySet()

        when {
            include.isNotEmpty() -> include
            exclude.isNotEmpty() -> modelDtoVariants - exclude
            else -> modelDtoVariants
        }
    }

    val propertyNominalTyping = fieldAnnotation?.arguments
        ?.find { it.name?.asString() == "nominalTyping" }
        ?.value?.toString()?.substringAfterLast('.')

    val finalNominalTyping = if (propertyNominalTyping != null && propertyNominalTyping != "INHERIT") {
        propertyNominalTyping
    } else {
        modelNominalTyping
    }

    val useSerializerAnnotation =
        propertyDeclaration.annotations.firstOrNull { it.isAnnotation(REPLICATE_WITH_SERIALIZER_ANNOTATION_NAME) }
    val forceContextualAnnotation =
        propertyDeclaration.annotations.firstOrNull { it.isAnnotation(REPLICATE_FORCE_CONTEXTUAL_ANNOTATION_NAME) }
    val customSerializer = useSerializerAnnotation?.arguments?.firstOrNull()?.value as? String
    val forceContextual = forceContextualAnnotation != null

    val ksType = propertyDeclaration.type.resolve()
    val typeInfo = KSTypeInfo.from(ksType, environment, resolver).toModelTypeInfo(customSerializer, forceContextual)
    val propertyAnnotations: List<AnnotationModel>? =
        propertyDeclaration.annotations.toAnnotationModels(frameworkDeclarations)

    val foreignDecl = resolver.getClassDeclarationByName(
        resolver.getKSNameFromString(typeInfo.qualifiedName)
    )
    val isSourceForeignModel = foreignDecl?.annotations?.any { it.isAnnotation(MODEL_ANNOTATION_NAME) } == true
    val isGeneratedForeignModel = isGeneratedVariantContainer(foreignDecl)
    val isForeignModel = isSourceForeignModel || isGeneratedForeignModel

    environment.logger.logging(
        "processProperty name=${propertyDeclaration.simpleName.asString()} " +
                "qualified=${typeInfo.qualifiedName} isValueClass=${typeInfo.isValueClass} foreign=$isForeignModel"
    )

    return if (isForeignModel && foreignDecl != null) {
        createForeignProperty(
            propertyDeclaration,
            typeInfo,
            foreignDecl,
            propertyVariants,
            propertyAnnotations,
            finalNominalTyping
        )
    } else {
        RegularProperty(
            name = propertyDeclaration.simpleName.asString(),
            typeInfo = typeInfo,
            dtoVariants = propertyVariants,
            annotations = propertyAnnotations,
            nominalTyping = finalNominalTyping
        )
    }
}

private fun createForeignProperty(
    propertyDeclaration: KSPropertyDeclaration,
    typeInformation: TypeInfo,
    foreignModelDeclaration: KSClassDeclaration,
    dtoVariants: Set<DtoVariant>,
    annotations: List<AnnotationModel>?,
    nominalTyping: String?
): ForeignProperty {
    val simpleName = foreignModelDeclaration.simpleName.asString()
    val foreignModelNameForLookup = if (simpleName.endsWith("Schema")) {
        simpleName.removeSuffix("Schema")
    } else {
        simpleName
    }
    return ForeignProperty(
        name = propertyDeclaration.simpleName.asString(),
        typeInfo = typeInformation,
        foreignModelName = foreignModelNameForLookup,
        dtoVariants = dtoVariants,
        annotations = annotations,
        nominalTyping = nominalTyping
    )
}