package io.availe

import io.availe.models.Variant
import kotlin.reflect.KClass

enum class NominalTyping {
    ENABLED,
    DISABLED,
    INHERIT
}

object Replicate {
    @Target(AnnotationTarget.CLASS)
    annotation class Model(
        val variants: Array<Variant>,
        val nominalTyping: NominalTyping = NominalTyping.DISABLED
    )

    @Target(AnnotationTarget.PROPERTY)
    annotation class Property(
        val exclude: Array<Variant> = [],
        val include: Array<Variant> = [],
        val nominalTyping: NominalTyping = NominalTyping.INHERIT
    )

    @Repeatable
    @Target(AnnotationTarget.CLASS)
    annotation class Apply(
        val annotations: Array<KClass<out Annotation>>,
        val include: Array<Variant> = [],
        val exclude: Array<Variant> = []
    )

    @Target(AnnotationTarget.PROPERTY)
    annotation class WithSerializer(val with: String)

    @Target(AnnotationTarget.PROPERTY)
    annotation class ForceContextual

    @Target(AnnotationTarget.CLASS)
    annotation class SchemaVersion(val number: Int)

    @Target(AnnotationTarget.CLASS)
    annotation class Hide
}