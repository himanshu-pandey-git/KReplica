package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
sealed class Property {
    abstract val name: String
    abstract val typeInfo: TypeInfo
    abstract val variants: Set<Variant>
    abstract val annotations: List<AnnotationModel>?
    abstract val nominalTyping: String?
}

@Serializable
data class RegularProperty(
    override val name: String,
    override val typeInfo: TypeInfo,
    override val variants: Set<Variant>,
    override val annotations: List<AnnotationModel>? = null,
    override val nominalTyping: String? = null
) : Property()

@Serializable
data class ForeignProperty(
    override val name: String,
    override val typeInfo: TypeInfo,
    val foreignModelName: String,
    override val variants: Set<Variant>,
    override val annotations: List<AnnotationModel>? = null,
    override val nominalTyping: String? = null
) : Property()