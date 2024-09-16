package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * The [list of items](https://developers.google.com/keep/api/reference/rest/v1/notes#listcontent) for a single list note.
 *
 * @property listItems The items in the list. The number of items must be less than 1,000.
 */
data class ListContent(
    @SerialName("listItems")
    val listItems: List<ListItem>,
)