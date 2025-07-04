# KReplica

KReplica is a DTO generator for KMM and Kotlin JVM. It runs automatically during Kotlin compilation.

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

## Quick Start

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
@OptIn(ExperimentalUuidApi::class)
@Replicate.Model(
    variants = [Variant.BASE, Variant.CREATE, Variant.PATCH], // required argument
    nominalTyping = NominalTyping.ENABLED // disabled by default
)
private interface UserAccount {
    // This property inherits all of @Replicate.Model's arguments
    val emailAddress: String

    // This property is only included in the BASE variant
    @Replicate.Property(include = [Variant.BASE])
    val id: Uuid

    // This property is excluded from the CREATE variant
    @Replicate.Property(exclude = [Variant.CREATE])
    val banReason: Patchable<List<String?>>

    // We opt out of nominalTyping for this property
    @Replicate.Property(nominalTyping = NominalTyping.DISABLED)
    val userDescription: String?
}
```

See the [generated code](docs/EXAMPLES.md#example-non-versioned).

See the [patchable file](docs/EXAMPLES.md#patchable)


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

See the [generated code](docs/EXAMPLES.md#example-versioned)

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

See the [generated code](docs/EXAMPLES.md#example-versioned-w-schema-version-annotation)

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

See the [generated code](docs/EXAMPLES.md#example-serializable)

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
@Replicate.Model(variants = [Variant.BASE])
@Deprecated("Use NewUserAccount instead")
private interface UserAccount {
    @Deprecated("Use newId instead")
    val id: Int
}
```

See the [generated code](docs/EXAMPLES.md#directly-applying-annotations)

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

## Contextual nested models

Say that you previously defined `UserAccount`:

```kotlin
private interface UserAccount

@Replicate.Model(variants = [Variant.BASE, Variant.CREATE, Variant.PATCH], nominalTyping = NominalTyping.ENABLED)
private interface V1 : UserAccount {
    val id: Int
}
```

Now you want `UserAccount` to be included in `AdminAccount`, but in a particular format:

- `AdminAccount` base variant should include `UserAccount` base variant
- `AdminAccount` create variant should include `UserAccount` create variant
- `AdminAccount` patch variant should include `UserAccount` patch variant

You could use the `Replicate.Property` annotation to manually configure each field, or you can take advantage of
contextual nested models:

```kotlin
@Replicate.Model(variants = [Variant.BASE, Variant.CREATE, Variant.PATCH])
private interface AdminAccount {
    val user: UserAccountSchema.V1
}
```

See the [generated code](docs/EXAMPLES.md#contextual-nested-model-versioned)

Or if `UserAccount` was a non-versioned schema:

```kotlin
@Replicate.Model(variants = [Variant.BASE, Variant.CREATE, Variant.PATCH])
private interface AdminAccount {
    val user: UserAccountSchema
}
```

See the [generated code](docs/EXAMPLES.md#contextual-nested-model-non-versioned)

## FAQ

### Can a `Replication.Property` have a broader replication than its `Replication.Model`?

No. The replication of all children must be a subset of the parent (⊆), including for nested models. Otherwise, KReplica
will error and log the offending fields.
This rule ensures fail-fast feedback. If you restrict a parent’s replication but forget to update a child field, you’ll
get an immediate build-time error.

### If a @Replicate.Model has another @Replicate.Model as a field, does the order of compilation matter?

No. KReplica actually cleans the build folder at the start of each run (which ensures no stale data). To ensure that
nested contextuals work, KReplica uses two-pass compilation. Prior to the main compilation, stub files of all
`Replicate.Model` declarations, which is then deleted to avoid a redeclaration error.

### Why do all the examples use the private keyword (private interface)?

The `private` keyword is not required for KReplica to function. However, the KReplica interfaces are useless outside
of KReplica, so I prefer to private them so they don't contaminate the name space.

This is particularly important with versioned schemas, as the V[number] naming convention is repetitive and you cannot
redeclare interfaces. This does mean, however, that only one versioned schema can exist per file.