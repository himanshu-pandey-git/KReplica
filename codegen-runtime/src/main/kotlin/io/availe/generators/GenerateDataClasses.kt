package io.availe.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.availe.SCHEMA_SUFFIX
import io.availe.SERIALIZABLE_QUALIFIED_NAME
import io.availe.builders.*
import io.availe.models.*
import io.availe.utils.fieldsFor
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("io.availe.generators")

fun generateDataClasses(primaryModels: List<Model>, allModels: List<Model>, outputDir: File) {
    val modelsByName = allModels.associateBy { it.name }
    val primaryModelsByBaseName = primaryModels.groupBy { it.isVersionOf ?: it.name }
    primaryModelsByBaseName.forEach { (baseName, versions) ->
        generateSchemaFile(baseName, versions, modelsByName, outputDir)
    }
}

private fun generateSchemaFile(
    baseName: String,
    versions: List<Model>,
    modelsByName: Map<String, Model>,
    outputDir: File
) {
    val isVersioned = versions.first().isVersionOf != null
    val representativeModel = versions.first()
    val schemaFileName = (if (isVersioned) baseName else representativeModel.name) + SCHEMA_SUFFIX
    logger.info("Generating schema file: $schemaFileName in package ${representativeModel.packageName}")
    val fileBuilder = FileSpec.builder(representativeModel.packageName, schemaFileName)
        .addFileComment(FILE_HEADER_COMMENT)
        .addOptInMarkersForModels(versions)

    val isGloballySerializable =
        representativeModel.annotationConfigs.any { it.annotation.qualifiedName == SERIALIZABLE_QUALIFIED_NAME }

    if (isVersioned) {
        val topLevelClassBuilder = TypeSpec.interfaceBuilder(schemaFileName)
            .addModifiers(KModifier.SEALED)
            .apply {
                if (isGloballySerializable) {
                    addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                }
            }
            .addKdoc(TOP_LEVEL_CLASS_KDOC, baseName)

        val allVariants = versions.flatMap { it.dtoVariants }.toSet()
        allVariants.forEach { variant ->
            val interfaceName = "${variant.suffix}Variant"
            val variantInterfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(KModifier.SEALED)
                .addSuperinterface(ClassName(representativeModel.packageName, schemaFileName))
            topLevelClassBuilder.addType(variantInterfaceBuilder.build())
        }

        versions.forEach { version ->
            val dtos =
                generateDataTransferObjects(version, modelsByName)
            val versionClass = TypeSpec.interfaceBuilder(version.name)
                .addModifiers(KModifier.SEALED)
                .addSuperinterface(ClassName(version.packageName, schemaFileName))
                .apply {
                    version.annotations.forEach { annotationModel ->
                        addAnnotation(buildAnnotationSpec(annotationModel))
                    }
                    val isVersionSerializable =
                        version.annotationConfigs.any { it.annotation.qualifiedName == SERIALIZABLE_QUALIFIED_NAME }
                    if (isVersionSerializable) {
                        addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                    }
                }
                .addKdoc(generateVersionBoxKdoc(version.name, version.schemaVersion!!))
                .addTypes(dtos)
                .build()
            topLevelClassBuilder.addType(versionClass)
        }
        fileBuilder.addType(topLevelClassBuilder.build())
    } else {
        val model = versions.first()
        val dtos = generateDataTransferObjects(model, modelsByName)
        val schemaInterfaceName = ClassName(representativeModel.packageName, schemaFileName)
        val schemaBuilder = TypeSpec.interfaceBuilder(schemaInterfaceName)
            .addModifiers(KModifier.SEALED)
            .addKdoc("A sealed hierarchy representing all variants of the %L data model.", baseName)

        model.annotations.forEach { annotationModel ->
            schemaBuilder.addAnnotation(buildAnnotationSpec(annotationModel))
        }

        if (isGloballySerializable) {
            schemaBuilder.addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
        }

        dtos.forEach { dtoSpec ->
            val dtoBuilder = dtoSpec.toBuilder()
                .addSuperinterface(schemaInterfaceName)

            DtoVariant.entries.find { it.suffix == dtoSpec.name }?.let { variant ->
                val globalVariantInterfaceBase = when (variant) {
                    DtoVariant.DATA -> KReplicaDataVariant::class.asClassName()
                    DtoVariant.CREATE -> KReplicaCreateVariant::class.asClassName()
                    DtoVariant.PATCH -> KReplicaPatchVariant::class.asClassName()
                }
                val parameterizedGlobalVariant = globalVariantInterfaceBase.parameterizedBy(schemaInterfaceName)
                dtoBuilder.addSuperinterface(parameterizedGlobalVariant)
            }

            schemaBuilder.addType(dtoBuilder.build())
        }
        fileBuilder.addType(schemaBuilder.build())
    }
    fileBuilder.build().writeTo(outputDir)
}

private fun generateDataTransferObjects(
    model: Model,
    modelsByName: Map<String, Model>
): List<TypeSpec> {
    return model.dtoVariants.mapNotNull { variant ->
        val fields = fieldsFor(model, variant)
        if (fields.isNotEmpty()) {
            buildDataTransferObjectClass(
                model = model,
                properties = fields,
                dtoVariant = variant,
                modelsByName = modelsByName,
                coreInterfaceSpec = null
            )
        } else null
    }
}

private fun FileSpec.Builder.addOptInMarkersForModels(
    models: List<Model>
): FileSpec.Builder {
    val distinctMarkers = models.flatMap { it.optInMarkers }.distinct()
    if (distinctMarkers.isNotEmpty()) {
        val format = distinctMarkers.joinToString(", ") { "%T::class" }
        val args = distinctMarkers.map { fq ->
            val pkg = fq.substringBeforeLast('.')
            val cls = fq.substringAfterLast('.')
            ClassName(pkg, cls)
        }.toTypedArray()
        addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember(format, *args)
                .build()
        )
    }
    return this
}