package io.availe.builders

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.availe.SERIALIZABLE_PATCHABLE_CLASS_NAME
import io.availe.models.*

internal fun resolveTypeNameForProperty(
    property: Property,
    dtoVariant: DtoVariant,
    model: Model,
    modelsByName: Map<String, Model>,
    isContainerSerializable: Boolean
): TypeName {
    val patchableClassName = if (model.isSerializable(dtoVariant)) {
        ClassName("io.availe.models", SERIALIZABLE_PATCHABLE_CLASS_NAME)
    } else {
        ClassName("io.availe.models", "Patchable")
    }

    val finalAutoContextualEnabled = property.autoContextual == AutoContextual.ENABLED

    val baseType = if (property is ForeignProperty) {
        buildRecursiveDtoTypeName(property.typeInfo, dtoVariant, modelsByName, isContainerSerializable)
    } else {
        buildTypeNameWithContextual(property.typeInfo, isContainerSerializable, finalAutoContextualEnabled)
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