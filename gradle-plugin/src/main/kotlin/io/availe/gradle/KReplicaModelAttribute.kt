package io.availe.gradle

import org.gradle.api.attributes.Attribute

internal object KReplicaModelAttribute {
    val KREPLICA_MODEL_TYPE_ATTRIBUTE: Attribute<String> =
        Attribute.of("io.availe.kreplica.model.type", String::class.java)

    const val MODELS_JSON_TYPE = "models-json"
}