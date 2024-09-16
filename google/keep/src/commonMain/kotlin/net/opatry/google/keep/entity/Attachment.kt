package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * An [attachment](https://developers.google.com/keep/api/reference/rest/v1/notes#attachment) to a note.
 *
 * @property name The resource name;
 * @property mimeType The MIME types (IANA media types) in which the attachment is available.
 */
data class Attachment(
    @SerialName("name")
    val name: String,
    @SerialName("mimeType")
    val mimeType: List<String>,
)