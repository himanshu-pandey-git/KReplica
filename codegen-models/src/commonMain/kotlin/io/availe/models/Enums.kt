package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
enum class Replication { NONE, PATCH, CREATE, BOTH }

@Serializable
enum class DtoVariant(val suffix: String) {
    BASE("Data"),
    CREATE("CreateRequest"),
    PATCH("PatchRequest")
}