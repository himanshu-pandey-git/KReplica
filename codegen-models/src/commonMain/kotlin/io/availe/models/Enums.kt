package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
enum class Replication { NONE, PATCH, CREATE, BOTH }

@Serializable
enum class Variant(val suffix: String) {
    BASE("Data"),
    CREATE("CreateRequest"),
    PATCH("PatchRequest")
}