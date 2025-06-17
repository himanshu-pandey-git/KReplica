package io.availe

import io.availe.models.Variant
import kotlin.reflect.KClass

enum class NominalTyping {
    ENABLED,
    DISABLED,
    INHERIT
}

@Target(AnnotationTarget.CLASS)
annotation class Replicate(
    val variants: Array<Variant> = [Variant.BASE, Variant.CREATE, Variant.PATCH],
    val nominalTyping: NominalTyping = NominalTyping.DISABLED
)

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class ApplyAnnotations(
    val annotations: Array<KClass<out Annotation>>,
    val include: Array<Variant> = [],
    val exclude: Array<Variant> = []
)

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class ExtendVariant(
    val variant: Variant,
    val with: KClass<*>
)

@Target(AnnotationTarget.PROPERTY)
annotation class ReplicateProperty(
    val exclude: Array<Variant> = [],
    val include: Array<Variant> = [],
    val nominalTyping: NominalTyping = NominalTyping.INHERIT
)