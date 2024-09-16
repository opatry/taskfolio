package net.opatry.google.keep.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * The [response when listing a page of notes](https://developers.google.com/keep/api/reference/rest/v1/notes/list#response-body).
 *
 * @property notes A page of notes.
 * @property nextPageToken Next page's pageToken field.
 */
data class ListNotesResponse(
    @SerialName("notes")
    val notes: List<Note> = emptyList(),
    @SerialName("nextPageToken")
    val nextPageToken: String = "",
)