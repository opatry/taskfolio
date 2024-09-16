package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * The [block of text](https://developers.google.com/keep/api/reference/rest/v1/notes#textcontent) for a single text section or list item.
 *
 * @property text The text of the note. The limits on this vary with the specific field using this type.
 */
data class TextContent(
    @SerialName("text")
    val text: String,
)