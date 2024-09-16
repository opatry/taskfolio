package net.opatry.google.keep.entity


import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * A single [note](https://developers.google.com/keep/api/reference/rest/v1/notes#resource:-note).
 *
 * @property name Output only. The resource name of this note. See general note on identifiers in KeepService.
 * @property createTime Output only. When this note was created.
 * @property updateTime Output only. When this note was last modified.
 * @property trashTime Output only. When this note was trashed. If [isTrashed], the note is eventually deleted. If the note is not trashed, this field is not set (and the [isTrashed] field is `false`).
 * @property isTrashed Output only. true if this note has been trashed. If trashed, the note is eventually deleted.
 * @property attachments Output only. The attachments attached to this note.
 * @property permissions Output only. The list of permissions set on the note. Contains at least one entry for the note owner.
 * @property title The title of the note. Length must be less than 1,000 characters.
 * @property body The body of the note.
 */
data class Note(
    @SerialName("name")
    val name: String,
    @Serializable(with = ProtobufTimestampSerializer::class)
    @SerialName("createTime")
    val createTime: Instant = Clock.System.now(),
    @SerialName("updateTime")
    val updateTime: Instant = Clock.System.now(),
    @SerialName("trashTime")
    val trashTime: Instant? = null,
    @SerialName("trashed")
    val isTrashed: Boolean = false,
    @SerialName("attachments")
    val attachments: List<Attachment> = emptyList(),
    @SerialName("permissions")
    val permissions: List<Permission> = emptyList(),
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: Section,
)