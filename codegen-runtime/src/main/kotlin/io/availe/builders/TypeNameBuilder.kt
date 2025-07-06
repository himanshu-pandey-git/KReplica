package io.availe.builders

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.availe.models.INTRINSIC_SERIALIZABLES
import io.availe.models.TypeInfo

internal fun buildTypeNameWithContextual(
    typeInfo: TypeInfo,
    isContainerSerializable: Boolean,
    autoContextualEnabled: Boolean
): TypeName {
    return buildTypeNameRecursive(typeInfo, parentRequiresContext = false, isContainerSerializable, autoContextualEnabled)
}

private fun buildTypeNameRecursive(
    typeInfo: TypeInfo,
    parentRequiresContext: Boolean,
    isContainerSerializable: Boolean,
    autoContextualEnabled: Boolean
): TypeName {
    val isIntrinsic = INTRINSIC_SERIALIZABLES.contains(typeInfo.qualifiedName)

    val needsContextNow =
        parentRequiresContext || (autoContextualEnabled && typeInfo.requiresContextual)

    val rawType = typeInfo.qualifiedName.asClassName()

    val typeArguments = typeInfo.arguments.map { arg ->
        buildTypeNameRecursive(arg, needsContextNow, isContainerSerializable, autoContextualEnabled)
    }

    val parameterizedType = if (typeArguments.isEmpty()) rawType else rawType.parameterizedBy(typeArguments)

    val shouldAnnotate = isContainerSerializable && (needsContextNow && !isIntrinsic)

    val finalType = if (shouldAnnotate) {
        val contextualAnnotation = AnnotationSpec.builder(ClassName("kotlinx.serialization", "Contextual")).build()
        parameterizedType.copy(annotations = parameterizedType.annotations + contextualAnnotation)
    } else {
        parameterizedType
    }

    return finalType.copy(nullable = typeInfo.isNullable)
}

fun TypeInfo.toTypeName(isContainerSerializable: Boolean, autoContextualEnabled: Boolean): TypeName {
    return buildTypeNameWithContextual(this, isContainerSerializable, autoContextualEnabled)
}