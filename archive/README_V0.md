# KReplica

KReplica is a nominally typed DTO generator for KMM and Kotlin JVM. KReplica generates replications, a base data class, a create data class, and a patch data class.

## 🚀 Quick Start

Add KSP and KReplica plugins to your Gradle module (`build.gradle.kts`):

```kotlin
plugins {
  id("com.google.devtools.ksp") version "2.1.20-2.0.1"
  id("io.availe.kreplica") version "1.0.0"
}
```

KReplica also has an optional Gradle Kotlin DSL. For example:

```Kotlin
kreplica {
    fromContext(project(projects.shared.path))
    generatePatchable = true
    serializePatchable = true
}
```

If no flags are provided, by default generatePatchable is true, serializePatchable is false, and fromContext is null.

fromContext allows KReplica to generate code defined in other modules, useful in some contexts.


## ✨ Example (non-versioned)
There are four replication levels:

- Replication.NONE (generates base data class)
- Replication.CREATE (generates base and create data classes)
- Replication.PATCH (generates base and patch data classes)
- Replication.BOTH (generates base, create, and patch data classes)

@ModelGen has the following parameters: replication (required), annotations (optional), optInMarkers (optional).

Note while it might feel different writing ```Serializable::class``` as opposed to ```@Serializable```, you still benefit from IDE autocomplete, at least in IntelliJ.

```kotlin
@ModelGen(
    replication = Replication.BOTH,
    annotations = [Serializable::class],
    optInMarkers = [ExperimentalUuidApi::class]
)
private interface UserAccount {
    @FieldGen(Replication.NONE)
    @Contextual
    val allIds: List<Uuid>
    val name: String
}
```

Here is the generated code. Notice how userId is only present in the base data class, but not in the Create Request or Patch Request.

This is useful if say you want userId to be created by the database. Here you cannot accidentally assign a userId to a Create Request.

```kotlin
@file:OptIn(ExperimentalUuidApi::class)

@Serializable
public data class UserAccountData(
  public val id: UserAccountId,
  public val name: UserAccountName,
)

@Serializable
public data class UserAccountCreateRequest(
  public val name: UserAccountName,
)

@Serializable
public data class UserAccountPatchRequest(
  public val name: Patchable<UserAccountName> = Patchable.Unchanged,
)

// Value classes
@JvmInline
@Serializable
public value class UserAccountId(
  @Contextual
  public val `value`: Uuid,
)

@JvmInline
@Serializable
public value class UserAccountName(
  public val `value`: String,
)

```

Tangential, but here's what a patchable file looks like:

```kotlin
@Serializable
public sealed class Patchable<out T> {
  @Serializable
  public object Unchanged : Patchable<Nothing>()

  @Serializable
  public data class Set<T>(
    public val `value`: T,
  ) : Patchable<T>()
}

```

## ✨ Example (versioned)

KReplica can also create versioned seal classes. To do so, simply declare a @ModelGen that implement an interface. For example:

```kotlin
private interface User

@ModelGen(replication = Replication.NONE,)
private interface V1: User {
    val name: String
}

@ModelGen(replication = Replication.NONE,)
private interface V2: User {
    @Contextual
    val id: Int
    val name: String
}
```

Here is the generated code:
```kotlin
public sealed class UserSchema {
    // Version 1
  public sealed class V1 : UserSchema() {
    public data class Data(
      public val name: UserName,
      public val schemaVersion: UserSchemaVersion = UserSchemaVersion(1),
    ) : V1()
  }

    // Version 2
  public sealed class V2 : UserSchema() {
    public data class Data(
      public val id: UserId,
      public val name: UserName,
      public val schemaVersion: UserSchemaVersion = UserSchemaVersion(2),
    ) : V2()
  }
}

// Shared value classes
@JvmInline
public value class UserId(
  @Contextual
  public val `value`: Int,
)

@JvmInline
public value class UserName(
  public val `value`: String,
)

@JvmInline
public value class UserSchemaVersion(
  public val `value`: Int,
)

```

## FAQ
### What are all the annotations in KReplica?
- @ModelGen, @FieldGen (covered previously)
- @Hide (prevents a @ModelGen from being generated)
- @SchemaVersion (is required for *versioned* @ModelGens if you do not follow the V1, V2, V... naming convention)
### Did you account for edge cases?
- If a field's name is reused with a different type across a *versioned* schema (e.g., String vs Int), a distinct value class is generated per field version.
- If a field is already a value class, then its existent value class will be used instead of generating a new one.
- All KReplica build files are cleaned at the start of each run, preventing stale data.
- Nested models and generic types are supported.
### Can a field have a broader replication than its parent @ModelGen?
No. The replication of all children must be a subset of the parent (⊆), including for nested models. Otherwise, KReplica will error and log the offending fields.
This rule ensures fail-fast feedback. If you restrict a parent’s replication but forget to update a child field, you’ll get an immediate build-time error.
### If a @ModelGen has another @ModelGen as a field, does the order of compilation matter?
No. KReplica generates a models.json file as an intermediary representation, which is then checked for validity. While the IDE might show an error that said referenced model does not exist yet, you can just build, and KReplica will handle dependencies automatically.
### Why do all the examples use the private keyword (private interface)?
The ```private``` keyword is not required for KReplica to function. However, the KReplica interfaces are useless outside of KReplica, so I prefer to private them so they don't contaminate the name space.
### Can nested data classes have context-specific nested field properties?
If a ModelGen references a *versioned* @ModelGen, you can write this:
```kotlin
@ModelGen(Replication.BOTH)
private interface UserAccount {
    val conversation: ConversationSchema.V1
}
```
What this does is that it ensures the following:
- User Account base class has a Conversation Version 1 base class
- User Account patch class has a Conversation Version 1 patch class
- User Account create class has a Conversation Version 1 create class

If for whatever reason you do NOT want context-specific, you can do this.
```kotlin
@ModelGen(Replication.BOTH)
interface UserAccount {
    val conversation: ConversationSchema.V1.Data
}
```

This ensures all UserAccount variants contain a Conversation Version 1 base class.

Note that if you want to use the context-specific version, and you're referencing a @ModelGen defined in another Gradle module, you need to use ```fromContext()``` (see quick start section). 
### How do I use annotations with arguments?
The current annotations/opt-in-marker system was designed since some annotations cannot be used on interfaces. For example, this is NOT valid:
```kotlin
@Serializable
private interface Car
```

However, some annotations can be utilized on interfaces.
### Is KReplica actively maintained?
Yes. I use KReplica in my own internal projects, so it receives updates and bug fixes as needed.

The codebase was originally part of a larger project before being separated into its own repository, so it has been developed and refined based on my own use cases. Broader feedback and contributions are always welcome!
