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


class TaskModelTest {
    @Test
    fun `parse empty tasks from json`() = runTest {
        val tasks = loadJsonAsObject<ResourceListResponse<Task>>("/tasks_empty.json")
        assertEquals(
            ResourceListResponse(
                kind = ResourceType.Tasks,
                etag = "\"LTkzOTI5MzMyNQ\"",
                items = emptyList()
            ),
            tasks
        )
    }

    @Test
    fun `parse tasks from json`() = runTest {
        val tasks = loadJsonAsObject<ResourceListResponse<Task>>("/tasks_with_completed_and_hidden.json")
        assertEquals(
            ResourceListResponse(
                kind = ResourceType.Tasks,
                etag = "\"LTkzOTI5MzMyNQ\"",
                items = listOf(
                    Task(
                        kind = ResourceType.Task,
                        id = "dnBVd2IwZUlMcjZWNU84YQ",
                        etag = "\"LTkzOTI4MTk4Nw\"",
                        title = "First task TODO",
                        updatedDate = Instant.parse("2024-10-26T08:48:57.000Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/dnBVd2IwZUlMcjZWNU84YQ",
                        position = "00000000000000000000",
                        status = Task.Status.NeedsAction,
                        dueDate = Instant.parse("2024-10-28T00:00:00.000Z"),
                        links = emptyList(),
                        webViewLink = "https://tasks.google.com/task/vpUwb0eILr6V5O8a?sa=6"
                    ),
                    Task(
                        kind = ResourceType.Task,
                        id = "M3R6eUVFQzJJUzQzZC12Qg",
                        etag = "\"LTk0NDMxMTUxOQ\"",
                        title = "A completed task",
                        updatedDate = Instant.parse("2024-10-26T07:25:08.000Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/M3R6eUVFQzJJUzQzZC12Qg",
                        position = "09999998270072491951",
                        status = Task.Status.Completed,
                        dueDate = Instant.parse("2024-10-26T00:00:00.000Z"),
                        completedDate = Instant.parse("2024-10-26T07:25:08.000Z"),
                        isHidden = true,
                        links = emptyList(),
                        webViewLink = "https://tasks.google.com/task/3tzyEEC2IS43d-vB?sa=6"
                    ),
                    Task(
                        kind = ResourceType.Task,
                        id = "OTJOZTNPYnJjbWQ0OF9mVQ",
                        etag = "\"LTE3MDM5ODAyMDc\"",
                        title = "🎵 with emoji",
                        updatedDate = Instant.parse("2024-10-17T12:23:59.000Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/OTJOZTNPYnJjbWQ0OF9mVQ",
                        position = "00000000000000000002",
                        status = Task.Status.NeedsAction,
                        dueDate = Instant.parse("2024-10-28T00:00:00.000Z"),
                        links = emptyList(),
                        webViewLink = "https://tasks.google.com/task/92Ne3Obrcmd48_fU?sa=6"
                    ),
                    Task(
                        kind = ResourceType.Task,
                        id = "d254c01jY1NBNEpydUJJdw",
                        etag = "\"LTE4OTYzOTA2NjI\"",
                        title = "Deleted task",
                        updatedDate = Instant.parse("2024-10-15T06:57:09.000Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/d254c01jY1NBNEpydUJJdw",
                        position = "00000000000000000000",
                        status = Task.Status.NeedsAction,
                        isDeleted = true,
                        links = emptyList(),
                        webViewLink = "https://tasks.google.com/task/wnxsMccSA4JruBIw?sa=6"
                    ),
                    Task(
                        kind = ResourceType.Task,
                        id = "T0dZdThPNDR1RUdydUdCbQ",
                        etag = "\"LTE5ODE3MDM5Mjg\"",
                        title = "Task with notes & due date",
                        updatedDate = Instant.parse("2024-10-14T07:15:16.000Z"),
                        selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/T0dZdThPNDR1RUdydUdCbQ",
                        position = "00000000000000000004",
                        notes = "Some notes",
                        status = Task.Status.NeedsAction,
                        dueDate = Instant.parse("2024-10-28T00:00:00.000Z"),
                        links = emptyList(),
                        webViewLink = "https://tasks.google.com/task/OGYu8O44uEGruGBm?sa=6"
                    )
                )
            ),
            tasks
        )
    }

    @Test
    fun `parse task from json`() = runTest {
        val task = loadJsonAsObject<Task>("/task.json")
        assertEquals(
            Task(
                kind = ResourceType.Task,
                id = "dnBVd2IwZUlMcjZWNU84YQ",
                etag = "\"LTkzOTI4MTk4Nw\"",
                title = "First task TODO",
                updatedDate = Instant.parse("2024-10-26T08:48:57.000Z"),
                selfLink = "https://www.googleapis.com/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks/dnBVd2IwZUlMcjZWNU84YQ",
                position = "00000000000000000000",
                status = Task.Status.NeedsAction,
                dueDate = Instant.parse("2024-10-28T00:00:00.000Z"),
                links = emptyList(),
                webViewLink = "https://tasks.google.com/task/vpUwb0eILr6V5O8a?sa=6"
            ),
            task
        )
    }
}
