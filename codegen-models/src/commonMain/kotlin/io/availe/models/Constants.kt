package io.availe.models

const val SERIALIZABLE_ANNOTATION_FQN: String = "kotlinx.serialization.Serializable"

val INTRINSIC_SERIALIZABLES: Set<String> = setOf(
    "kotlin.String",
    "kotlin.Char",
    "kotlin.Boolean",
    "kotlin.Byte",
    "kotlin.Short",
    "kotlin.Int",
    "kotlin.Long",
    "kotlin.Float",
    "kotlin.Double",
    "kotlin.Unit",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.Map",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterable",
    "kotlin.Array",
    "kotlin.BooleanArray",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlinx.serialization.Polymorphic",
    "kotlinx.serialization.Contextual",
    "kotlin.uuid.Uuid"
)