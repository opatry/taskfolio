package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * The [content](https://developers.google.com/keep/api/reference/rest/v1/notes#section) of the note.
 *
 * Union field `Content`. The section's content must be one of these value types. `Content` can be only one of the following:
 * @property text Used if this section's content is a block of text. The length of the text content must be less than 20,000 characters.
 * @property list Used if this section's content is a list.
 */
data class Section(
    @SerialName("text")
    val text: TextContent? = null,
    @SerialName("list")
    val list: ListContent? = null,
)

// FIXME handle union type with a sealed class