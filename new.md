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

This example covers how to use the `Replicate` and `ReplicateProperty` annotations.

```kotlin
@Replicate(
    variants = [Variant.BASE, Variant.CREATE, Variant.PATCH], // required argument
    nominalTyping = NominalTyping.ENABLED // disabled by default
)
private interface UserAccount {
    // This property inherits all of @Replicate's arguments
    val email_address: String

    // This property is only included in the BASE variant
    @ReplicateProperty(include = [Variant.BASE])
    val id: UUID

    // This property is excluded from the CREATE variant
    @ReplicateProperty(exclude = [Variant.CREATE])
    val ban_reason: Option<String>

    // We opt out of nominalTyping for this property
    @ReplicateProperty(nominalTyping = NominalTyping.DISABLED)
    val user_description: String?
}
```

## Example (versioned)

To version a `Replicate` declaration, create a base interface (e.g. `UserAccount`) and extend it with V\<number\>
interfaces (e.g. `V1`, `V2`) to track model changes.

```kotlin
private interface UserAccount

@Replicate(variants = [Variant.BASE])
private interface V1 : UserAccount {
    val id: Int
}

@Replicate(variants = [Variant.BASE, Variant.PATCH])
private interface V2 : UserAccount {
    val id: Int
    val name: String
}
```

If you wish to not follow the V\<number\> naming convention, you must use the `SchemaVersion` annotation to manually
specify a version number.

```kotlin
private interface UserAccount

@Replicate(variants = [Variant.BASE, Variant.PATCH])
@SchemaVersion(1)
private interface NewAccount : UserAccount {
    val id: Int
    val name: String
}
```

Note all versioned declarations automatically inject a `schema_version` property into the generated DTOs.

## Example (serializable)

Interfaces cannot directly implement some annotations, including `Serializable`. Instead, you can use Note
`ApplyAnnotations`.

```kotlin
@Replicate(variants = [Variant.BASE, Variant.PATCH])
@ApplyAnnotations(annotations = [Serializable::class])
private interface UserAccount {
    val id: Int
}
```

Note that `ApplyAnnotations` can also take include/exclude arguments, if you want an annotation to only be applied to a
specific variant. Additionally, `ApplyAnnotations` is repeatable and takes a list of annotations.

The drawback of `ApplyAnnotations` is that the IDE no longer warns when `Contextual` is needed. To address this,
KReplica recursively applies
`Contextual` in generated code, so it works regardless of generic nesting.

Types exempt from `Contextual` are whitelisted in `codegen/src/commonMain/kotlin/io/availe/models/Constants.kt`.

For manual control, use the annotation `@UseSerializers`. It works identically to `@Serializable(with = ...)`, but is
needed since `@Serializable` isn’t used directly.

If you wish to force a property to use `Contextual`, you may use the annotation `@ForceContextual`.

## Directly applying annotations

If the annotation can be applied on interfaces, you can directly use it without the need for `ApplyAnnotations`. For
example:

```kotlin
@Replicate(variants = [Variant.BASE, Variant.PATCH])
@Deprecated("Use NewUserAccount instead")
private interface UserAccount {
    @Deprecated("Use newId instead")
    val id: Int
}
```

## The hide annotation

The `@Hide` annotation stops a `Replicate` declaration from being generated. It's mainly for temporarily testing how
code removal affects the system—but use it as you see fit.

```kotlin
@Replicate(variants = [Variant.BASE])
@Hide
private interface UserAccount {
    val id: Int
}
```