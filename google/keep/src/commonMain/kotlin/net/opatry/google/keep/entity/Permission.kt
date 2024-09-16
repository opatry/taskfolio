package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * A single [permission](https://developers.google.com/keep/api/reference/rest/v1/notes#permission) on the note. Associates a `member` with a `role`.
 *
 * @property name Output only. The resource name.
 * @property role The role granted by this permission. The role determines the entity’s ability to read, write, and share notes.
 * @property email The email associated with the member. If set on create, the `email` field in the [User] or [Group] message must either be empty or match this field. On read, may be unset if the member does not have an associated email.
 * @property isDeleted Output only. Whether this member has been deleted. If the member is recovered, this value is set to false and the recovered member retains the role on the note.
 * Union field `member`. Specifies the identity granted the role. Member is unset if the member has been deleted. `member` can be only one of the following:
 * @property user Output only. The user to whom this role applies.
 * @property group Output only. The group to which this role applies.
 * @property family Output only. The Google Family to which this role applies.
 */
data class Permission(
    @SerialName("name")
    val name: String,
    @SerialName("role")
    val role: Role,
    @SerialName("email")
    val email: String,
    @SerialName("deleted")
    val isDeleted: Boolean,
    @SerialName("user")
    val user: User? = null,
    @SerialName("group")
    val group: Group? = null,
    @SerialName("family")
    val family: Family? = null,
)

// FIXME handle union type with a sealed class