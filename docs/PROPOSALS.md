## Mapping

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