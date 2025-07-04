/*
 * Copyright (c) 2025 Olivier Patry
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.opatry.google.tasks.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.Task.Status
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * https://developers.google.com/tasks/reference/rest/v1/tasks#resource:-task
 *
 * @property kind Output only. Type of the resource. This is always [ResourceType.Task].
 * @property id Task identifier.
 * @property etag ETag of the resource.
 * @property title Title of the task. Maximum length allowed: 1024 characters.
 * @property updatedDate Output only. Last modification time of the task (as a RFC 3339 timestamp).
 * @property selfLink Output only. URL pointing to this task. Used to retrieve, update, or delete this task.
 * @property parent Output only. Parent task identifier. This field is omitted if it is a top-level task. Use the [TasksApi.move] method to move the task under a different parent or to the top level. A parent task can never be an assigned task (from Chat Spaces, Docs). This field is read-only.
 * @property position Output only. String indicating the position of the task among its sibling tasks under the same parent task or at the top level. If this string is greater than another task's corresponding position string according to lexicographical ordering, the task is positioned after the other task under the same parent task (or at the top level). Use the [TasksApi.move] method to move the task to another position.
 * @property notes Notes describing the task. Tasks assigned from Google Docs cannot have notes. Optional. Maximum length allowed: 8192 characters.
 * @property status Status of the task. This is either [Status.NeedsAction] or [Status.Completed].
 * @property dueDate Due date of the task (as a RFC 3339 timestamp). Optional. The due date only records date information; the time portion of the timestamp is discarded when setting the due date. It isn't possible to read or write the time that a task is due via the API.
 * @property completedDate Completion date of the task (as a RFC 3339 timestamp). This field is omitted if the task has not been completed.
 * @property isDeleted Flag indicating whether the task has been deleted. For assigned tasks this field is read-only. They can only be deleted by calling [TasksApi.delete], in which case both the assigned task and the original task (in Docs or Chat Spaces) are deleted. To delete the assigned task only, navigate to the assignment surface and unassign the task from there. The default is `false`.
 * @property isHidden Flag indicating whether the task is hidden. This is the case if the task had been marked completed when the task list was last cleared. The default is `false`. This field is read-only.
 * @property links Output only. Collection of links. This collection is read-only.
 * @property webViewLink Output only. An absolute link to the task in the Google Tasks Web UI.
 * @property assignmentInfo Output only. Context information for assigned tasks. A task can be assigned to a user, currently possible from surfaces like Docs and Chat Spaces. This field is populated for tasks assigned to the current user and identifies where the task was assigned from. This field is read-only.
 */
@Serializable
data class Task(
    @SerialName("kind")
    val kind: ResourceType = ResourceType.Task,
    @SerialName("id")
    val id: String = "",
    @SerialName("etag")
    val etag: String = "",
    @SerialName("title")
    val title: String,
    @Serializable(with = ProtobufTimestampSerializer::class)
    @SerialName("updated")
    val updatedDate: Instant = Clock.System.now(),
    @SerialName("selfLink")
    val selfLink: String = "",
    @SerialName("parent")
    val parent: String? = null,
    @SerialName("position")
    val position: String = "",
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("status")
    val status: Status = Status.NeedsAction,
    @SerialName("due")
    @Serializable(with = ProtobufTimestampSerializer::class)
    val dueDate: Instant? = null,
    @Serializable(with = ProtobufTimestampSerializer::class)
    @SerialName("completed")
    val completedDate: Instant? = null,
    @SerialName("deleted")
    val isDeleted: Boolean = false,
    @SerialName("hidden")
    val isHidden: Boolean = false,
    @SerialName("links")
    val links: List<Link> = emptyList(),
    @SerialName("webViewLink")
    val webViewLink: String = "",
    @SerialName("assignmentInfo")
    val assignmentInfo: AssignmentInfo? = null,
) {
    val isCompleted = completedDate != null && status == Status.Completed

    @Serializable
    enum class Status {
        @SerialName("needsAction")
        NeedsAction,

        @SerialName("completed")
        Completed,
    }

    /**
     * @property type Type of the link, e.g. "email".
     * @property description The description. In HTML speak: Everything between <a> and </a>.
     * @property link The URL.
     */
    @Serializable
    data class Link(
        @SerialName("type")
        val type: String,
        @SerialName("description")
        val description: String,
        @SerialName("link")
        val link: String
    )
}

/**
 * Factory function to create a new [TaskList] exposing only relevant parameters.
 *
 * @property title Title of the task. Maximum length allowed: 1024 characters.
 * @property notes Notes describing the task. Tasks assigned from Google Docs cannot have notes. Optional. Maximum length allowed: 8192 characters.
 * @property status Status of the task. This is either [Status.NeedsAction] or [Status.Completed].
 * @property dueDate Due date of the task (as a RFC 3339 timestamp). Optional. The due date only records date information; the time portion of the timestamp is discarded when setting the due date. It isn't possible to read or write the time that a task is due via the API.
 * @property completedDate Completion date of the task (as a RFC 3339 timestamp). This field is omitted if the task has not been completed.
 */
fun Task(
    title: String,
    notes: String? = null,
    status: Status = Status.NeedsAction,
    dueDate: Instant? = null,
    completedDate: Instant? = null
): Task {
    require(title.length <= 1024) { "Title length must be at most 1024 characters" }
    require(notes == null || notes.length <= 8192) { "Notes length must be at most 8192 characters" }
    // need to artificially define an extra parameter to call data class ctor instead of recursive call
    return Task(id = "", title = title, notes = notes, status = status, dueDate = dueDate, completedDate = completedDate)
}