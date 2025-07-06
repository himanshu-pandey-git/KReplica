package io.availe.builders

import com.squareup.kotlinpoet.*
import io.availe.*
import io.availe.models.*

internal fun Model.isSerializable(dtoVariant: DtoVariant): Boolean {
    return annotationConfigs.any { config ->
        dtoVariant in config.variants && config.annotation.qualifiedName == SERIALIZABLE_QUALIFIED_NAME
    }
}

private fun getPatchableClassName(model: Model, dtoVariant: DtoVariant): ClassName {
    return if (model.isSerializable(dtoVariant)) {
        ClassName(MODELS_PACKAGE_NAME, SERIALIZABLE_PATCHABLE_CLASS_NAME)
    } else {
        ClassName(MODELS_PACKAGE_NAME, PATCHABLE_CLASS_NAME)
    }
}

fun buildDataTransferObjectClass(
    model: Model,
    properties: List<Property>,
    dtoVariant: DtoVariant,
    valueClassNames: Map<Pair<String, String>, String>,
    existingValueClasses: Set<String>,
    modelsByName: Map<String, Model>,
    coreInterfaceSpec: TypeSpec?
): TypeSpec {
    val generatedClassName = dtoVariant.suffix

    val typeSpecBuilder = TypeSpec.classBuilder(generatedClassName).addModifiers(KModifier.DATA)

    model.annotations.forEach { annotationModel ->
        typeSpecBuilder.addAnnotation(buildAnnotationSpec(annotationModel))
    }

    model.annotationConfigs.filter { dtoVariant in it.variants }.forEach { config ->
        typeSpecBuilder.addAnnotation(buildAnnotationSpec(config.annotation))
    }

    if (model.isVersionOf != null) {
        val schemaName = model.isVersionOf!! + "Schema"

        val versionInterface = ClassName(model.packageName, schemaName, model.name)
        typeSpecBuilder.addSuperinterface(versionInterface)

        val variantKindInterface = ClassName(model.packageName, schemaName, "${dtoVariant.suffix}Variant")
        typeSpecBuilder.addSuperinterface(variantKindInterface)
    }

    val constructorBuilder = FunSpec.constructorBuilder()
    val isSerializable = model.isSerializable(dtoVariant)
    properties.forEach { property ->
        val typeName =
            resolveTypeNameForProperty(
                property,
                dtoVariant,
                model,
                valueClassNames,
                existingValueClasses,
                modelsByName,
                isContainerSerializable = isSerializable
            )

        val paramBuilder = ParameterSpec.builder(property.name, typeName)

        var annotationsToApply = property.annotations

        val isWrappedInValueClass = (property.nominalTyping == NominalTyping.ENABLED &&
                !propertyShouldSkipWrapping(property, existingValueClasses) &&
                property is RegularProperty)

        val shouldFilterContextual = (dtoVariant == DtoVariant.PATCH) || isWrappedInValueClass

        if (shouldFilterContextual) {
            annotationsToApply =
                annotationsToApply.filterNot { it.qualifiedName == "kotlinx.serialization.Contextual" }
        }

        annotationsToApply = annotationsToApply.filterNot { it.qualifiedName == OPT_IN_QUALIFIED_NAME }

        annotationsToApply.forEach { annotationModel ->
            paramBuilder.addAnnotation(buildAnnotationSpec(annotationModel))
        }

        if (property.name == SCHEMA_VERSION_PROPERTY_NAME && dtoVariant != DtoVariant.PATCH) {
            val isWrapped = (property.nominalTyping == NominalTyping.ENABLED &&
                    !propertyShouldSkipWrapping(property, existingValueClasses) &&
                    property is RegularProperty)

            if (isWrapped) {
                paramBuilder.defaultValue("%T(%L)", typeName, model.schemaVersion)
            } else {
                paramBuilder.defaultValue("%L", model.schemaVersion)
            }
        }

        if (dtoVariant == DtoVariant.PATCH) {
            val patchableClassName = getPatchableClassName(model, dtoVariant)
            paramBuilder.defaultValue(
                "%T.%L",
                patchableClassName,
                UNCHANGED_OBJECT_NAME
            )
        }
        constructorBuilder.addParameter(paramBuilder.build())

        val propertySpecBuilder = PropertySpec.builder(property.name, typeName).initializer(property.name)

        typeSpecBuilder.addProperty(propertySpecBuilder.build())
    }
    typeSpecBuilder.primaryConstructor(constructorBuilder.build())

    return typeSpecBuilder.build()
}