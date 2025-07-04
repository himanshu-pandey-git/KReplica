package io.availe.utils

import io.availe.models.DtoVariant
import io.availe.models.Model
import io.availe.models.Property

fun fieldsFor(model: Model, dtoVariant: DtoVariant): List<Property> =
    model.properties.filter { dtoVariant in it.dtoVariants }