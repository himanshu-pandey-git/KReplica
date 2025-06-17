package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
data class ExtendConfigModel(
    val superclassFqName: String,
    val variant: Variant
)

@Serializable
data class AnnotationConfigModel(
    val annotation: AnnotationModel,
    val variants: Set<Variant>
)

@Serializable
data class Model(
    val name: String,
    val packageName: String,
    val properties: List<Property>,
    val variants: Set<Variant>,
    val annotationConfigs: List<AnnotationConfigModel> = emptyList(),
    val optInMarkers: List<String>? = null,
    val isVersionOf: String? = null,
    val schemaVersion: Int? = null,
    val nominalTyping: String? = null,
    val extendConfigs: List<ExtendConfigModel> = emptyList()
) {
    init {
        require(properties.isNotEmpty()) {
            "Model validation failed for '$name': Model interfaces cannot be empty and must contain at least one property."
        }
        val invalidProperties = properties.filter {
            !this.variants.containsAll(it.variants)
        }
        require(invalidProperties.isEmpty()) {
            val count = invalidProperties.size
            val pluralS = if (count == 1) "" else "s"
            val noun = if (count == 1) "property" else "properties"
            val verb = if (count == 1) "is" else "are"
            val propertiesReport = invalidProperties.joinToString("\n") {
                " - Property: '${it.name}' (has variants '${it.variants}')"
            }
            """
            Invalid property variant$pluralS found in model '$name':
            The model's variants are '${this.variants}', which does not fully contain the property's variants.
            The following $noun $verb invalid:
            $propertiesReport
            """.trimIndent()
        }
    }
}