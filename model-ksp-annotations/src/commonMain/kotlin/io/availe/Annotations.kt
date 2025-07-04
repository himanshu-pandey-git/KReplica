package io.availe

import io.availe.models.DtoVariant
import kotlin.reflect.KClass

enum class NominalTyping {
    ENABLED,
    DISABLED,
    INHERIT
}

object Replicate {
    @Target(AnnotationTarget.CLASS)
    annotation class Model(
        val variants: Array<DtoVariant>,
        val nominalTyping: NominalTyping = NominalTyping.DISABLED
    )

    @Target(AnnotationTarget.PROPERTY)
    annotation class Property(
        val exclude: Array<DtoVariant> = [],
        val include: Array<DtoVariant> = [],
        val nominalTyping: NominalTyping = NominalTyping.INHERIT
    )

    @Repeatable
    @Target(AnnotationTarget.CLASS)
    annotation class Apply(
        val annotations: Array<KClass<out Annotation>>,
        val include: Array<DtoVariant> = [],
        val exclude: Array<DtoVariant> = []
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