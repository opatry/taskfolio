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

package net.opatry.tasks.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.tasks.data.util.respondNoNetwork
import net.opatry.tasks.data.util.respondWithTaskLists
import net.opatry.tasks.data.util.respondWithTasks
import net.opatry.tasks.data.util.runTaskRepositoryTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


class TaskRepositorySyncTest {
    @Test
    fun `sync remote task lists`() {
        MockEngine { request ->
            when (val encodedPath = request.url.encodedPath) {
                "/tasks/v1/users/@me/lists" -> respondWithTaskLists(
                    "MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow" to "My tasks",
                    "OXl0d1JibXgyeW1zWWFIMw" to "Other tasks"
                )

                "/tasks/v1/lists/MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow/tasks" -> respondWithTasks("dnBVd2IwZUlMcjZWNU84YQ" to "First task TODO")
                "/tasks/v1/lists/OXl0d1JibXgyeW1zWWFIMw/tasks" -> respondWithTasks("M3R6eUVFQzJJUzQzZC12Qg" to "Another task")
                else -> fail("Unexpected request: $request ($encodedPath)")
            }
        }.use { mockEngine ->
            runTaskRepositoryTest(mockEngine) { repository ->
                val initialTaskLists = repository.getTaskLists().firstOrNull()
                assertEquals(0, initialTaskLists?.size, "There shouldn't be any task list at start")
                repository.sync()

                val taskLists = repository.getTaskLists().firstOrNull()
                assertEquals(2, taskLists?.size)
                assertContentEquals(listOf("My tasks", "Other tasks"), taskLists?.map(TaskListDataModel::title))

                val firstTaskListTasks = taskLists?.get(0)?.tasks
                assertEquals(1, firstTaskListTasks?.size)
                assertEquals("First task TODO", firstTaskListTasks?.firstOrNull()?.title)

                val secondTaskListTasks = taskLists?.get(1)?.tasks
                assertEquals(1, secondTaskListTasks?.size)
                assertEquals("Another task", secondTaskListTasks?.firstOrNull()?.title)
            }
        }
    }

    @Test
    fun `backend error while syncing should do nothing`() {
        MockEngine {
            respondError(HttpStatusCode.Forbidden)
        }.use { mockEngine ->
            runTaskRepositoryTest(mockEngine) { repository ->
                repository.sync()
                assertEquals(0, repository.getTaskLists().firstOrNull()?.size)
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.Forbidden, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `task CRUD without network should create a local only task`() {
        MockEngine {
            respondNoNetwork()
        }.use { mockEngine ->
            runTaskRepositoryTest(mockEngine) { repository ->
                repository.createTaskList("Task list")
                assertEquals(1, repository.getTaskLists().firstOrNull()?.size)
            }

            assertEquals(0, mockEngine.responseHistory.size)
        }
    }

    @Test
    fun `local only tasks are synced at next sync`() {
        var requestCount = 0
        MockEngine { request ->
            ++requestCount
            when {
                requestCount == 1
                        && request.method == HttpMethod.Post
                        && request.url.encodedPath == "/tasks/v1/users/@me/lists"
                -> respondNoNetwork()

                requestCount == 2
                        && request.method == HttpMethod.Get
                        && request.url.encodedPath == "/tasks/v1/users/@me/lists"
                -> respondWithTaskLists()

                requestCount == 3
                        && request.method == HttpMethod.Post
                        && request.url.encodedPath == "/tasks/v1/users/@me/lists"
                -> respondWithTaskLists("MTAwNDEyMDI1NDY0NDEwNzQ0NDI6MDow" to "Task list")

                else -> fail("Unexpected request: $request")
            }
        }.use { mockEngine ->
            runTaskRepositoryTest(mockEngine) { repository ->
                // for first request, no network
                repository.createTaskList("Task list")
                val taskList = repository.getTaskLists().firstOrNull()?.firstOrNull()
                assertNotNull(taskList)
                assertEquals(0, mockEngine.responseHistory.size)

                // network is considered back, sync should trigger fetch & push requests
                repository.sync()
                assertEquals(2, mockEngine.responseHistory.size)
            }
        }
    }
}