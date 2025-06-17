package io.availe

import java.io.File

internal val OUTPUT_DIRECTORY = File("build/generated-src/kotlin-poet")
internal const val MODELS_PACKAGE_NAME = "io.availe.models"

internal const val SCHEMA_SUFFIX = "Schema"
internal const val PATCHABLE_CLASS_NAME = "Patchable"
internal const val SERIALIZABLE_PATCHABLE_CLASS_NAME = "SerializablePatchable"
internal const val UNCHANGED_OBJECT_NAME = "Unchanged"

internal const val SCHEMA_VERSION_PROPERTY_NAME = "schemaVersion"
internal const val VALUE_PROPERTY_NAME = "value"

internal const val OPT_IN_QUALIFIED_NAME = "kotlin.OptIn"
internal const val SERIALIZABLE_QUALIFIED_NAME = "kotlinx.serialization.Serializable"