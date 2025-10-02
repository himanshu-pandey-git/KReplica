package io.availe.models

internal val INTRINSIC_SERIALIZABLES: Set<String> = setOf(
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

object KReplicaPaths {
    const val MODELS_JSON_FILE = "models.json"
}