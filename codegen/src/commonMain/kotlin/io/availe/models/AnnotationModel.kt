package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
data class AnnotationModel(
    val qualifiedName: String,
    val arguments: Map<String, AnnotationArgument> = emptyMap()
)

@Serializable
sealed class AnnotationArgument {
    @Serializable
    data class StringValue(val value: String) : AnnotationArgument()
    @Serializable
    data class LiteralValue(val value: String) : AnnotationArgument()
}