package net.opatry.google.keep.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.opatry.google.keep.entity.Role.Editor
import net.opatry.google.keep.entity.Role.Owner
import net.opatry.google.keep.entity.Role.Unspecified

@Serializable
/**
 * Defines the various [roles](https://developers.google.com/keep/api/reference/rest/v1/notes#role) an entity can have.
 *
 * @property Unspecified An undefined role.
 * @property Owner A role granting full access. This role cannot be added or removed. Defined by the creator of the note.
 * @property Editor A role granting the ability to contribute content and modify note permissions.
 */
enum class Role {
    @SerialName("ROLE_UNSPECIFIED")
    Unspecified,
    @SerialName("OWNER")
    Owner,
    @SerialName("EDITOR")
    Editor,
}