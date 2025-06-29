# KReplica

KReplica is a DTO generator for KMM and Kotlin JVM.

### Features:

* **Variant generation:** From a single interface, specify up to three variants to generate:
    * `BASE`: For read-only data representation
    * `CREATE`: For object creation requests
    * `PATCH`: For update requests, wraps properties in a `Patchable` type.
* **Granular control:** Enable or disable specific features or variants at both the model and property levels.
* **Nominal Typing:** Automatically wrap primitive types in value classes for added type safety and clarity.
* **Schema Versioning:** Defines a sealed interface for each schema version, enabling exhaustive when expressions.
* **Plain Kotlin:** Emits plain Kotlin source files to your build directory, free of framework-specific code or runtime
  dependencies.

## 🚀 Quick Start

Add the KSP and KReplica plugins to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.21-2.0.1" // Use a KSP version that matches your Kotlin version
    id("io.availe.kreplica") version "1.0.0"
}
```

## Example (non-versioned)

This example covers how to use the `Replicate.Model` and `Replicate.Property` annotations.

```kotlin
@Replicate.Model(
    variants = [Variant.BASE, Variant.CREATE, Variant.PATCH], // required argument
    nominalTyping = NominalTyping.ENABLED // disabled by default
)
private interface UserAccount {
    // This property inherits all of @Replicate.Model's arguments
    val emailAddress: String

    // This property is only included in the BASE variant
    @Replicate.Property(include = [Variant.BASE])
    val id: UUID

    // This property is excluded from the CREATE variant
    @Replicate.Property(exclude = [Variant.CREATE])
    val banReason: Option<String>

    // We opt out of nominalTyping for this property
    @Replicate.Property(nominalTyping = NominalTyping.DISABLED)
    val userDescription: String?
}
```

## Example (versioned)

To version a `Replicate.Model` declaration, create a base interface (e.g. `UserAccount`) and extend it with V\<number\>
interfaces (e.g. `V1`, `V2`) to track model changes.

```kotlin
private interface UserAccount

@Replicate.Model(variants = [Variant.BASE])
private interface V1 : UserAccount {
    val id: Int
}

@Replicate.Model(variants = [Variant.BASE, Variant.PATCH])
private interface V2 : UserAccount {
    val id: Int
    val name: String
}
```

If you wish to not follow the V\<number\> naming convention, you must use the `Replicate.SchemaVersion` annotation to
manually
specify a version number.

```kotlin
private interface UserAccount

@Replicate.Model(variants = [Variant.BASE, Variant.PATCH])
@Replicate.SchemaVersion(1)
private interface NewAccount : UserAccount {
    val id: Int
    val name: String
}
```

Note all versioned declarations automatically inject a `schema_version` property into the generated DTOs.

## Example (serializable)

Interfaces cannot directly implement some annotations, including `Serializable`. Instead, you can use Note
`Replicate.Apply`.

```kotlin
@Replicate.Model(variants = [Variant.BASE, Variant.PATCH])
@Replicate.Apply(annotations = [Serializable::class])
private interface UserAccount {
    val id: Int
}
```

Note that `Replicate.Apply` can also take include/exclude arguments, if you want an annotation to only be
applied to a
specific variant. Additionally, `Replicate.Apply` is repeatable and takes a list of annotations.

The drawback of `Replicate.Apply` is that the IDE no longer warns when `Contextual` is needed. To address
this,
KReplica recursively applies
`Contextual` in generated code, so it works regardless of generic nesting.

Types exempt from `Contextual` are whitelisted in `codegen-models/src/commonMain/kotlin/io/availe/models/Constants.kt`.

For manual control, use the `@Replicate.WithSerializer(with = ...)` annotation. It works identically as
`@Serializable(with = ...)`
and emits
the latter annotation in the generated code.

If you wish to force a property to use `Contextual`, you may use the annotation `@Replicate.ForceContextual`.

## Directly applying annotations

If the annotation can be applied on interfaces, you can directly use it without the need for `Replicate.Apply`. For
example:

```kotlin
@Replicate.Model(variants = [Variant.BASE, Variant.PATCH])
@Deprecated("Use NewUserAccount instead")
private interface UserAccount {
    @Deprecated("Use newId instead")
    val id: Int
}
```

## The hide annotation

The `@Replicate.Hide` annotation stops a `Replicate.Model` declaration from being generated. It's mainly for temporarily
testing how
code removal affects the system—but use it as you see fit.

```kotlin
@Replicate.Model(variants = [Variant.BASE])
@Replicate.Hide
private interface UserAccount {
    val id: Int
}
```