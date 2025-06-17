package io.availe.utils

import io.availe.models.Model
import io.availe.models.Property
import io.availe.models.Variant

fun fieldsFor(model: Model, variant: Variant): List<Property> =
    model.properties.filter { variant in it.variants }