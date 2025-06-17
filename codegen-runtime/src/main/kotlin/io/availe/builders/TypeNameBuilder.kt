package io.availe.builders

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.availe.models.INTRINSIC_SERIALIZABLES
import io.availe.models.TypeInfo

internal fun buildTypeNameWithContextual(typeInfo: TypeInfo): TypeName {
    return buildTypeNameRecursive(typeInfo, parentRequiresContext = false)
}

private fun buildTypeNameRecursive(typeInfo: TypeInfo, parentRequiresContext: Boolean): TypeName {
    val isIntrinsic = INTRINSIC_SERIALIZABLES.contains(typeInfo.qualifiedName)

    val needsContextNow = parentRequiresContext || typeInfo.forceContextual || typeInfo.requiresContextual

    val rawType = typeInfo.qualifiedName.asClassName()

    val typeArguments = typeInfo.arguments.map { arg ->
        buildTypeNameRecursive(arg, needsContextNow)
    }

    val parameterizedType = if (typeArguments.isEmpty()) rawType else rawType.parameterizedBy(typeArguments)

    val shouldAnnotate = typeInfo.forceContextual || (needsContextNow && !isIntrinsic)

    val finalType = if (shouldAnnotate) {
        val contextualAnnotation = AnnotationSpec.builder(ClassName("kotlinx.serialization", "Contextual")).build()
        parameterizedType.copy(annotations = parameterizedType.annotations + contextualAnnotation)
    } else {
        parameterizedType
    }

    return finalType.copy(nullable = typeInfo.isNullable)
}

fun TypeInfo.toTypeName(): TypeName {
    return buildTypeNameWithContextual(this)
}