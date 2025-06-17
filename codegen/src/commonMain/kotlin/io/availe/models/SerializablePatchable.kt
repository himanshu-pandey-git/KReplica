package io.availe.models

import kotlinx.serialization.Serializable

@Serializable
public sealed class SerializablePatchable<out T> {
    @Serializable
    public data class Set<out T>(public val value: T) : SerializablePatchable<T>()

    @Serializable
    public data object Unchanged : SerializablePatchable<Nothing>()
}