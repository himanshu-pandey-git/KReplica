package io.availe.utils

import io.availe.models.DtoVariant
import io.availe.models.ForeignProperty
import io.availe.models.Model
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.availe.utils.ValidateModels")

fun validateModelReplications(allModels: List<Model>) {
    val modelsByName = allModels.associateBy { it.name }
    val validationErrors = mutableListOf<String>()
    allModels.forEach { model ->
        if (DtoVariant.CREATE in model.dtoVariants && fieldsFor(model, DtoVariant.CREATE).isEmpty()) {
            validationErrors.add(createEmptyVariantError(model, DtoVariant.CREATE))
        }
        if (DtoVariant.PATCH in model.dtoVariants && fieldsFor(model, DtoVariant.PATCH).isEmpty()) {
            validationErrors.add(createEmptyVariantError(model, DtoVariant.PATCH))
        }
        model.properties
            .filterIsInstance<ForeignProperty>()
            .forEach { foreignProperty ->
                val targetModelName = foreignProperty.foreignModelName
                val targetModel = modelsByName[targetModelName]
                    ?: error("Unknown referenced model '$targetModelName' in ${model.name}")

                if (DtoVariant.CREATE in foreignProperty.dtoVariants && !(DtoVariant.CREATE in targetModel.dtoVariants)) {
                    validationErrors.add(createDependencyError(model, foreignProperty, targetModel, DtoVariant.CREATE))
                }
                if (DtoVariant.PATCH in foreignProperty.dtoVariants && !(DtoVariant.PATCH in targetModel.dtoVariants)) {
                    validationErrors.add(createDependencyError(model, foreignProperty, targetModel, DtoVariant.PATCH))
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

private fun createEmptyVariantError(model: Model, dtoVariant: DtoVariant): String {
    return """
    Model '${model.name}' has invalid configuration.
    It is declared to support the ${dtoVariant.name} variant, but it contains no properties for this variant. This would result in an empty '${model.name}${dtoVariant.suffix}' class.
    """.trimIndent()
}

private fun createDependencyError(
    parentModel: Model,
    violatingProperty: ForeignProperty,
    targetModel: Model,
    dtoVariant: DtoVariant
): String {
    val parentVariantClassName = "${parentModel.name}${dtoVariant.suffix}"
    val nestedVariantClassName = "${targetModel.name}${dtoVariant.suffix}"
    return """
    Cannot generate '$parentVariantClassName': required nested model '$nestedVariantClassName' cannot be generated.
    Details:
        Parent Model      : ${parentModel.name} (variants: ${parentModel.dtoVariants})
        Variant Requested : ${dtoVariant.name}
        Nested Property   : ${violatingProperty.name} (type: ${targetModel.name})
    
    Why:
        The parent model '${parentModel.name}' is configured to generate a '${dtoVariant.name}' variant.
        This variant includes the property '${violatingProperty.name}', which refers to the model '${targetModel.name}'.
        However, '${targetModel.name}' (variants: ${targetModel.dtoVariants}) does not support the '${dtoVariant.name}' variant.
    
    To fix this, either add '${dtoVariant.name}' to the variants of '${targetModel.name}', or adjust the variants of the '${violatingProperty.name}' property.
    """.trimIndent()
}