package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * Describes a single [user](https://developers.google.com/keep/api/reference/rest/v1/notes#user).
 *
 * @property email The user's email.
 */
data class User(
    @SerialName("email")
    val email: String,
)