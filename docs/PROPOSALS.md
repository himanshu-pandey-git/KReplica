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

private interface UserAccount

// The DTO declaration
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

private interface UserAccount

// The mapper class with the mapping logic.
class V1Mapper(override val source: UserModel) : V1 {
    override val id: Int get() = source.id
    override val fullName: String get() = "${source.firstName} ${source.lastName}"
}

// The DTO interface declaration.
@Replicate.Model(
    variants = [DtoVariant.DATA, DtoVariant.CREATE],
    mapperClass = V1Mapper::class
)
private interface V1 : UserAccount, KReplicaMapper<UserModel> {
    // Shape is declared here. The compiler ensures V1Mapper implements it.
    @Replicate.Property(include = [DtoVariant.DATA])
    val id: Int
    val fullName: String
}
```