package io.availe.builders

import com.squareup.kotlinpoet.*
import io.availe.OPT_IN_QUALIFIED_NAME
import io.availe.SERIALIZABLE_QUALIFIED_NAME
import io.availe.VALUE_PROPERTY_NAME
import io.availe.models.RegularProperty

fun buildValueClass(
    className: String,
    property: RegularProperty,
    isSerializable: Boolean,
    isAutoContextual: Boolean
): TypeSpec {
    val underlyingTypeName = property.typeInfo.toTypeName(
        isContainerSerializable = isSerializable,
        autoContextualEnabled = isAutoContextual
    )
    val constructorParameterBuilder = ParameterSpec.builder(VALUE_PROPERTY_NAME, underlyingTypeName)

    property.annotations
        .filterNot { it.qualifiedName == OPT_IN_QUALIFIED_NAME }
        .forEach { annotation ->
            constructorParameterBuilder.addAnnotation(buildAnnotationSpec(annotation))
        }
    return TypeSpec.classBuilder(className)
        .addAnnotation(JvmInline::class)
        .addModifiers(KModifier.VALUE)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(constructorParameterBuilder.build())
                .build()
        )
        .addProperty(
            PropertySpec.builder(VALUE_PROPERTY_NAME, underlyingTypeName)
                .initializer(VALUE_PROPERTY_NAME)
                .build()
        )
        .apply {
            if (isSerializable) {
                val serializableName = SERIALIZABLE_QUALIFIED_NAME.asClassName()
                addAnnotation(serializableName)
            }
        }
        .build()
}