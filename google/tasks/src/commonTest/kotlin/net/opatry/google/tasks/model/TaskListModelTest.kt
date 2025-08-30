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

import kotlinx.coroutines.test.runTest
import net.opatry.google.tasks.util.loadJsonAsObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TaskListModelTest {
    @Test
    fun `parse empty task lists from json`() = runTest {
        val taskLists = loadJsonAsObject<ResourceListResponse<TaskList>>("/tasklists_empty.json")
        assertEquals(
            ResourceListResponse(
                kind = ResourceType.TaskLists,
                etag = "\"MjEwOTM2OTcxOQ\"",
                items = emptyList()
            ),
            taskLists
        )
    }

    @Test
    fun `parse task lists from json`() = runTest {
        val taskLists = loadJsonAsObject<ResourceListResponse<TaskList>>("/tasklists.json")
        assertEquals(
            ResourceListResponse(
                kind = ResourceType.TaskLists,
                etag = "\"MjEwOTM2OTcxOQ\"",
                items = listOf(
                    TaskList(
                        kind = ResourceType.TaskList,
                        id = "MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow",
                        etag = "\"LTkzOTI5MzMyNQ\"",
                        title = "My tasks",
                        updatedDate = Instant.parse("2024-10-26T08:48:46.790Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/users/@me/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow"
                    ),
                    TaskList(
                        kind = ResourceType.TaskList,
                        id = "OXl0d1JibXgyeW1zWWFIMw",
                        etag = "\"LTE4NjM1MzE4NDk\"",
                        title = "Other tasks",
                        updatedDate = Instant.parse("2024-10-15T16:04:48.522Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw"
                    ),
                )
            ),
            taskLists
        )
    }

    @Test
    fun `parse task list from json`() = runTest {
        val taskList = loadJsonAsObject<TaskList>("/tasklist.json")
        assertEquals(
            TaskList(
                kind = ResourceType.TaskList,
                id = "OXl0d1JibXgyeW1zWWFIMw",
                etag = "\"LTE4NjM1MzE4NDk\"",
                title = "Other tasks",
                updatedDate = Instant.parse("2024-10-15T16:04:48.522Z"),
                selfLink = "https://www.googleapis.com/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw"
            ),
            taskList
        )
    }
}