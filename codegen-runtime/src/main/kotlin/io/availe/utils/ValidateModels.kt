package io.availe.utils

import io.availe.models.ForeignProperty
import io.availe.models.Model
import io.availe.models.Variant
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.availe.utils.ValidateModels")

fun validateModelReplications(allModels: List<Model>) {
    val modelsByName = allModels.associateBy { it.name }
    val validationErrors = mutableListOf<String>()
    allModels.forEach { model ->
        if (Variant.CREATE in model.variants && fieldsFor(model, Variant.CREATE).isEmpty()) {
            validationErrors.add(createEmptyVariantError(model, Variant.CREATE))
        }
        if (Variant.PATCH in model.variants && fieldsFor(model, Variant.PATCH).isEmpty()) {
            validationErrors.add(createEmptyVariantError(model, Variant.PATCH))
        }
        model.properties
            .filterIsInstance<ForeignProperty>()
            .forEach { foreignProperty ->
                val targetModelName = foreignProperty.foreignModelName
                val targetModel = modelsByName[targetModelName]
                    ?: error("Unknown referenced model '$targetModelName' in ${model.name}")

                if (Variant.CREATE in foreignProperty.variants && !(Variant.CREATE in targetModel.variants)) {
                    validationErrors.add(createDependencyError(model, foreignProperty, targetModel, Variant.CREATE))
                }
                if (Variant.PATCH in foreignProperty.variants && !(Variant.PATCH in targetModel.variants)) {
                    validationErrors.add(createDependencyError(model, foreignProperty, targetModel, Variant.PATCH))
                }
            }
    }
    if (validationErrors.isNotEmpty()) {
        val finalReport = "Model validation failed with ${validationErrors.size} error(s):\n\n" +
                validationErrors.joinToString("\n\n--------------------------------------------------\n\n")
        logger.error(finalReport)
        error(finalReport)
    }
}

private fun createEmptyVariantError(model: Model, variant: Variant): String {
    return """
    Model '${model.name}' has invalid configuration.
    It is declared to support the ${variant.name} variant, but it contains no properties for this variant. This would result in an empty '${model.name}${variant.suffix}' class.
    """.trimIndent()
}

private fun createDependencyError(
    parentModel: Model,
    violatingProperty: ForeignProperty,
    targetModel: Model,
    variant: Variant
): String {
    val parentVariantClassName = "${parentModel.name}${variant.suffix}"
    val nestedVariantClassName = "${targetModel.name}${variant.suffix}"
    return """
    Cannot generate '$parentVariantClassName': required nested model '$nestedVariantClassName' cannot be generated.
    Details:
        Parent Model      : ${parentModel.name} (variants: ${parentModel.variants})
        Variant Requested : ${variant.name}
        Nested Property   : ${violatingProperty.name} (type: ${targetModel.name})
    
    Why:
        The parent model '${parentModel.name}' is configured to generate a '${variant.name}' variant.
        This variant includes the property '${violatingProperty.name}', which refers to the model '${targetModel.name}'.
        However, '${targetModel.name}' (variants: ${targetModel.variants}) does not support the '${variant.name}' variant.
    
    To fix this, either add '${variant.name}' to the variants of '${targetModel.name}', or adjust the variants of the '${violatingProperty.name}' property.
    """.trimIndent()
}