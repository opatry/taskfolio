/*
 * Copyright (c) 2024 Olivier Patry
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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * https://developers.google.com/tasks/reference/rest/v1/tasklists#resource:-tasklist
 *
 * @property kind Output only. Type of the resource. This is always [ResourceType.TaskList].
 * @property id Task list identifier.
 * @property etag ETag of the resource.
 * @property title Title of the task list. Maximum length allowed: 1024 characters.
 * @property updatedDate Output only. Last modification time of the task list (as a RFC 3339 timestamp).
 * @property selfLink Output only. URL pointing to this task list. Used to retrieve, update, or delete this task list.
 */
data class TaskList(
    @SerialName("kind")
    val kind: ResourceType = ResourceType.TaskList,
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
)

/**
 * Factory function to create a new [TaskList] exposing only relevant parameters.
 *
 * @param title Title of the task list. Maximum length allowed: 1024 characters.
 */
fun TaskList(title: String): TaskList {
    require(title.length <= 1024) { "Title length must be at most 1024 characters" }
    // need to artificially define an extra parameter to call data class ctor instead of recursive call
    return TaskList(id = "", title = title)
}