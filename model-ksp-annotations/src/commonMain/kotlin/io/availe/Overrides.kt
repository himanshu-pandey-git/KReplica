package io.availe

@Target(AnnotationTarget.PROPERTY)
public annotation class ForceContextual

@Target(AnnotationTarget.PROPERTY)
public annotation class UseSerializer(val with: String)