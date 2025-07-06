package io.availe.builders

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.availe.SERIALIZABLE_PATCHABLE_CLASS_NAME
import io.availe.models.*

internal fun propertyShouldSkipWrapping(property: Property, existingValueClasses: Set<String>): Boolean {
    return property.typeInfo.isEnum ||
            property.typeInfo.isValueClass ||
            property.typeInfo.isDataClass ||
            existingValueClasses.contains(property.typeInfo.qualifiedName)
}

internal fun resolveTypeNameForProperty(
    property: Property,
    dtoVariant: DtoVariant,
    model: Model,
    valueClassNames: Map<Pair<String, String>, String>,
    existingValueClasses: Set<String>,
    modelsByName: Map<String, Model>,
    isContainerSerializable: Boolean
): TypeName {
    val patchableClassName = if (model.isSerializable(dtoVariant)) {
        ClassName("io.availe.models", SERIALIZABLE_PATCHABLE_CLASS_NAME)
    } else {
        ClassName("io.availe.models", "Patchable")
    }

    val finalAutoContextualEnabled = (property.autoContextual ?: model.autoContextual) == AutoContextual.ENABLED

    val baseType = if (property is ForeignProperty) {
        buildRecursiveDtoTypeName(property.typeInfo, dtoVariant, modelsByName, isContainerSerializable)
    } else {
        val useWrapping = property.nominalTyping == NominalTyping.ENABLED
        val skipWrapping = propertyShouldSkipWrapping(property, existingValueClasses)
        if (useWrapping && !skipWrapping) {
            valueClassNames[model.name to property.name]?.let { vcName ->
                ClassName(model.packageName, vcName)
            } ?: buildTypeNameWithContextual(property.typeInfo, isContainerSerializable, finalAutoContextualEnabled)
        } else {
            buildTypeNameWithContextual(property.typeInfo, isContainerSerializable, finalAutoContextualEnabled)
        }
    }

    if (dtoVariant == DtoVariant.PATCH) {
        return patchableClassName.parameterizedBy(baseType)
    }

    return baseType
}

private fun buildSimpleTypeName(typeInfo: TypeInfo): TypeName {
    val rawType = typeInfo.qualifiedName.asClassName()
    if (typeInfo.arguments.isEmpty()) {
        return rawType.copy(nullable = typeInfo.isNullable)
    }
    val typeArguments = typeInfo.arguments.map { buildSimpleTypeName(it) }
    return rawType.parameterizedBy(typeArguments).copy(nullable = typeInfo.isNullable)
}

internal fun buildRecursiveDtoTypeName(
    typeInfo: TypeInfo,
    dtoVariant: DtoVariant,
    modelsByName: Map<String, Model>,
    isCurrentContainerSerializable: Boolean
): TypeName {
    val simpleName = typeInfo.qualifiedName.substringAfterLast('.')
    val lookupKey = if (simpleName.endsWith("Schema")) simpleName.removeSuffix("Schema") else simpleName
    val targetModel = modelsByName[lookupKey]

    if (typeInfo.arguments.isEmpty()) {
        if (targetModel == null) {
            return buildTypeNameWithContextual(typeInfo, isCurrentContainerSerializable, true)
        }

        val finalDtoName = if (targetModel.isVersionOf != null) {
            ClassName(targetModel.packageName, "${targetModel.isVersionOf}Schema", targetModel.name, dtoVariant.suffix)
        } else {
            ClassName(targetModel.packageName, "${targetModel.name}Schema", dtoVariant.suffix)
        }
        return finalDtoName.copy(nullable = typeInfo.isNullable)
    } else {
        val rawType = typeInfo.qualifiedName.asClassName()
        val transformedArgs = typeInfo.arguments.map { arg ->
            val isNestedContainerSerializable =
                modelsByName[arg.qualifiedName]?.isSerializable(dtoVariant) ?: isCurrentContainerSerializable
            buildRecursiveDtoTypeName(arg, dtoVariant, modelsByName, isNestedContainerSerializable)
        }
        return rawType.parameterizedBy(transformedArgs).copy(nullable = typeInfo.isNullable)
    }
}