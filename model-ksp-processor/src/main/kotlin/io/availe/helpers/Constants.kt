package io.availe.helpers

import io.availe.Replicate

val MODEL_ANNOTATION_NAME: String = Replicate.Model::class.qualifiedName!!
val REPLICATE_APPLY_ANNOTATION_NAME: String = Replicate.Apply::class.qualifiedName!!
val REPLICATE_PROPERTY_ANNOTATION_NAME: String = Replicate.Property::class.qualifiedName!!
val REPLICATE_SCHEMA_VERSION_ANNOTATION_NAME: String = Replicate.SchemaVersion::class.qualifiedName!!
val REPLICATE_HIDE_ANNOTATION_NAME: String = Replicate.Hide::class.qualifiedName!!
val REPLICATE_FORCE_CONTEXTUAL_ANNOTATION_NAME: String = Replicate.ForceContextual::class.qualifiedName!!
val REPLICATE_WITH_SERIALIZER_ANNOTATION_NAME: String = Replicate.WithSerializer::class.qualifiedName!!

const val SCHEMA_VERSION_ARG: String = "number"
const val SCHEMA_VERSION_FIELD: String = "schemaVersion"
const val OPT_IN_ANNOTATION_NAME: String = "kotlin.OptIn"