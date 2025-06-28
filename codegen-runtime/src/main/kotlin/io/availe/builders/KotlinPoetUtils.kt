package io.availe.builders

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import io.availe.models.AnnotationArgument
import io.availe.models.AnnotationModel

internal fun String.asClassName(): ClassName {
    val cleanName = this.substringBefore('<').removeSuffix("?")
    val packageName = cleanName.substringBeforeLast('.')
    val simpleName = cleanName.substringAfterLast('.')
    return ClassName(packageName, simpleName)
}

internal fun buildAnnotationSpec(annotationModel: AnnotationModel): AnnotationSpec {
    val className = annotationModel.qualifiedName.asClassName()
    val builder = AnnotationSpec.builder(className)
    annotationModel.arguments.forEach { (argName, argVal) ->
        when (argVal) {
            is AnnotationArgument.StringValue -> builder.addMember("%L = %S", argName, argVal.value)
            is AnnotationArgument.LiteralValue -> builder.addMember("%L = %L", argName, argVal.value)
            is AnnotationArgument.AnnotationValue -> {
                val nestedAnnotationSpec = buildAnnotationSpec(argVal.value)
                builder.addMember("%L = %L", argName, nestedAnnotationSpec)
            }
        }
    }
    return builder.build()
}