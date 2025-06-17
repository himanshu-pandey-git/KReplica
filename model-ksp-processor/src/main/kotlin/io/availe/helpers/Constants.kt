package io.availe.helpers

import io.availe.*

val MODEL_ANNOTATION_NAME: String = Replicate::class.qualifiedName!!
const val APPLY_ANNOTATIONS_ANNOTATION_NAME: String = "io.availe.ApplyAnnotations"
const val EXTEND_VARIANT_ANNOTATION_NAME: String = "io.availe.ExtendVariant"
val FIELD_ANNOTATION_NAME: String = ReplicateProperty::class.qualifiedName!!
val SCHEMA_VERSION_ANNOTATION_NAME: String = SchemaVersion::class.qualifiedName!!
val HIDE_ANNOTATION_NAME: String = Hide::class.qualifiedName!!
val FORCE_CONTEXTUAL_ANNOTATION_NAME: String = ForceContextual::class.qualifiedName!!
val USE_SERIALIZER_ANNOTATION_NAME: String = UseSerializer::class.qualifiedName!!

const val SCHEMA_VERSION_ARG: String = "number"
const val SCHEMA_VERSION_FIELD: String = "schemaVersion"
const val OPT_IN_ANNOTATION_NAME: String = "kotlin.OptIn"