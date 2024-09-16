package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * Describes a single [group](https://developers.google.com/keep/api/reference/rest/v1/notes#group).
 *
 * @property email The group email.
 */
data class Group(
    @SerialName("email")
    val email: String,
)