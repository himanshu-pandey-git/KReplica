package io.availe.models

public sealed class Patchable<out T> {
    public data class Set<out T>(public val value: T) : Patchable<T>()
    public data object Unchanged : Patchable<Nothing>()
}