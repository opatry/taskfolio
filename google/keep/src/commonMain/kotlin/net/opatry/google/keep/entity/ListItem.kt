package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * A single [list item](https://developers.google.com/keep/api/reference/rest/v1/notes#listitem) in a note's list.
 *
 * @property childListItems If set, list of list items nested under this list item. Only one level of nesting is allowed.
 * @property text The text of this item. Length must be less than 1,000 characters.
 * @property isChecked Whether this item has been checked off or not.
 */
data class ListItem(
    @SerialName("childListItems")
    val childListItems: List<ListItem> = emptyList(),
    @SerialName("text")
    val text: TextContent,
    @SerialName("checked")
    val isChecked: Boolean
)