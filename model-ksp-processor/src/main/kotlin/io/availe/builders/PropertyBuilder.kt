package io.availe.builders

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.availe.helpers.*
import io.availe.models.*

internal fun processProperty(
    propertyDeclaration: KSPropertyDeclaration,
    modelDtoVariants: Set<DtoVariant>,
    modelAutoContextual: AutoContextual,
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

        val include =
            includeArg?.map { DtoVariant.valueOf((it as KSDeclaration).simpleName.asString()) }?.toSet() ?: emptySet()
        val exclude =
            excludeArg?.map { DtoVariant.valueOf((it as KSDeclaration).simpleName.asString()) }?.toSet() ?: emptySet()

        when {
            include.isNotEmpty() -> include
            exclude.isNotEmpty() -> modelDtoVariants - exclude
            else -> modelDtoVariants
        }
    }

    val propertyAutoContextual = fieldAnnotation?.arguments
        ?.find { it.name?.asString() == "autoContextual" }
        ?.let { AutoContextual.valueOf((it.value as KSDeclaration).simpleName.asString()) }

    val finalAutoContextual = if (propertyAutoContextual != null && propertyAutoContextual != AutoContextual.INHERIT) {
        propertyAutoContextual
    } else {
        modelAutoContextual
    }

    val ksType = propertyDeclaration.type.resolve()
    val typeInfo = KSTypeInfo.from(ksType, environment, resolver).toModelTypeInfo()
    val propertyAnnotations: List<AnnotationModel> =
        propertyDeclaration.annotations.toAnnotationModels(frameworkDeclarations)

    val foreignDecl = resolver.getClassDeclarationByName(
        resolver.getKSNameFromString(typeInfo.qualifiedName)
    )
    val isSourceForeignModel = foreignDecl?.annotations?.any { it.isAnnotation(MODEL_ANNOTATION_NAME) } == true

    if (isSourceForeignModel) {
        val propertyName = propertyDeclaration.simpleName.asString()
        val sourceInterfaceName = typeInfo.qualifiedName
        val parentInterfaceName =
            (propertyDeclaration.parent as? KSClassDeclaration)?.qualifiedName?.asString() ?: "Unknown Interface"
        val suggestedSchemaName = sourceInterfaceName + "Schema"

        fail(
            environment,
            """
            KReplica Validation Error in '$parentInterfaceName':
            Property '$propertyName' references the KReplica model interface '$sourceInterfaceName'.
            You must use the generated schema `${suggestedSchemaName}Schema` instead.
            

            Please change the property type from `$propertyName` to '${propertyName}Schema'.
            """.trimIndent()
        )
    }

    val isGeneratedForeignModel = isGeneratedVariantContainer(foreignDecl)

    environment.logger.logging(
        "processProperty name=${propertyDeclaration.simpleName.asString()} " +
                "qualified=${typeInfo.qualifiedName} isValueClass=${typeInfo.isValueClass} foreign=$isGeneratedForeignModel"
    )

    return if (isGeneratedForeignModel && foreignDecl != null) {
        createForeignProperty(
            propertyDeclaration,
            typeInfo,
            foreignDecl,
            propertyVariants,
            propertyAnnotations,
            finalAutoContextual
        )
    } else {
        RegularProperty(
            name = propertyDeclaration.simpleName.asString(),
            typeInfo = typeInfo,
            dtoVariants = propertyVariants,
            annotations = propertyAnnotations,
            autoContextual = finalAutoContextual
        )
    }
}

private fun createForeignProperty(
    propertyDeclaration: KSPropertyDeclaration,
    typeInformation: TypeInfo,
    foreignModelDeclaration: KSClassDeclaration,
    dtoVariants: Set<DtoVariant>,
    annotations: List<AnnotationModel>,
    autoContextual: AutoContextual
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
        autoContextual = autoContextual
    )
}