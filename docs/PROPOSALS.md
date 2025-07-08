For mapping, my current idea is to go with proposal 3. That said, my long term goal is proposal 1.

## Mapping (proposal 1):

The problem with this is KSP does not support this.

```kotlin
// The source data model to map from.
data class UserModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val street: String,
    val city: String
)
```

```kotlin
private interface KReplicaMapper<T> {
    val source: T
}
```

```kotlin
private interface UserAccount

@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
private interface V1 : UserAccount, KReplicaMapper<UserModel> {

    // Simple mapping.
    @Replicate.Property(include = [DtoVariant.DATA])
    val id: Int get() = source.id

    // Mapper combines two source fields.
    val fullName: String get() = "${source.firstName} ${source.lastName}"
}
```

## Mapping (proposal 2):

Fully compatible with KSP but less ideal.

```kotlin
// The source data model to map from.
data class UserModel(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val street: String,
    val city: String
)
```


```kotlin
private interface UserAccount

// The DTO interface declaration.
@Replicate.Model(
    variants = [DtoVariant.DATA, DtoVariant.CREATE],
    mapperClass = V1Mapper::class
)
private interface V1 : UserAccount, KReplicaMapper<UserModel> {
    @Replicate.Property(include = [DtoVariant.DATA])
    val id: Int
    val fullName: String
}
```

```kotlin
// The mapper class.
class V1Mapper(override val source: UserModel) : V1 {
    override val id: Int get() = source.id
    override val fullName: String get() = "${source.firstName} ${source.lastName}"
}
```

## Mapping (proposal 3):

Third-option is to simply have no mapper built into KReplica, and instead recommend Mappie, which can map fields automatically, but allows explict override if required.

```kotlin
@Replicate.Model(variants = [DtoVariant.DATA, DtoVariant.CREATE])
private interface UserAccountV1 {
    val id: Int
    val fullName: String

    companion object : ObjectMappie<UserModel, UserAccountV1_Data>() {
        override fun map(from: UserModel) = mapping {
            to::fullName fromValue "${from.firstName} ${from.lastName}"
        }
    }
}
```
