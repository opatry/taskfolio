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

package net.opatry.google.tasks.service

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.util.loadJson
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TasksApiTest {

    @Test
    fun `TasksApi list`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/tasks_with_completed_and_hidden.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val tasks = tasksApi.list("SOME_ID", showCompleted = true, showDeleted = true, showHidden = true, showAssigned = true)

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks", request.url.encodedPath)
            assertEquals(5, queryParams.names().size)
            assertEquals("20", queryParams["maxResults"])
            assertEquals("true", queryParams["showCompleted"])
            assertEquals("true", queryParams["showDeleted"])
            assertEquals("true", queryParams["showHidden"])
            assertEquals("true", queryParams["showAssigned"])
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(ResourceType.Tasks, tasks.kind)
            assertEquals(5, tasks.items.size)
            assertContentEquals(listOf("First task TODO", "A completed task", "🎵 with emoji", "Deleted task", "Task with notes & due date"), tasks.items.map(Task::title))
        }
    }

    @Test
    fun `TasksApi list failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.list("")
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi insert`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/task.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val task = tasksApi.insert("SOME_ID", Task(title = "First task TODO"))

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(ResourceType.Task, task.kind)
            assertEquals("First task TODO", task.title)
            assertEquals("dnBVd2IwZUlMcjZWNU84YQ", task.id)
        }
    }

    @Test
    fun `TasksApi insert failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.insert("", Task(title = ""))
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi get`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/task.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val task = tasksApi.get("SOME_ID", "dnBVd2IwZUlMcjZWNU84YQ")

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks/dnBVd2IwZUlMcjZWNU84YQ", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(ResourceType.Task, task.kind)
            assertEquals("First task TODO", task.title)
            assertEquals("dnBVd2IwZUlMcjZWNU84YQ", task.id)
        }
    }

    @Test
    fun `TasksApi get failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.get("", "")
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi delete`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.NoContent,
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            tasksApi.delete("SOME_ID", "dnBVd2IwZUlMcjZWNU84YQ")

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks/dnBVd2IwZUlMcjZWNU84YQ", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Delete, request.method)
        }
    }

    @Test
    fun `TasksApi delete failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.delete("", "")
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi patch`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/task.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val task = tasksApi.patch("SOME_ID", "dnBVd2IwZUlMcjZWNU84YQ", Task(title = "First task TODO"))

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks/dnBVd2IwZUlMcjZWNU84YQ", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals(ResourceType.Task, task.kind)
            assertEquals("First task TODO", task.title)
            assertEquals("dnBVd2IwZUlMcjZWNU84YQ", task.id)
        }
    }

    @Test
    fun `TasksApi patch failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.patch("", "", Task(title = ""))
                }
            }
        }
    }

    @Test
    fun `TasksApi update`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/task.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val task = tasksApi.update("SOME_ID", "dnBVd2IwZUlMcjZWNU84YQ", Task(title = "First task TODO"))

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks/dnBVd2IwZUlMcjZWNU84YQ", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Put, request.method)
            assertEquals(ResourceType.Task, task.kind)
            assertEquals("First task TODO", task.title)
            assertEquals("dnBVd2IwZUlMcjZWNU84YQ", task.id)
        }
    }

    @Test
    fun `TasksApi update failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.update("", "", Task(title = ""))
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi move`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/task.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            val task = tasksApi.move("SOME_ID", "dnBVd2IwZUlMcjZWNU84YQ", destinationTaskListId = "SOME_OTHER_ID")

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/tasks/dnBVd2IwZUlMcjZWNU84YQ/move", request.url.encodedPath)
            assertEquals(1, queryParams.names().size)
            assertEquals("SOME_OTHER_ID", queryParams["destinationTasklist"])
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(ResourceType.Task, task.kind)
            assertEquals("dnBVd2IwZUlMcjZWNU84YQ", task.id)
        }
    }

    @Test
    fun `TasksApi move failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.move("", "")
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }

    @Test
    fun `TasksApi clear`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.NoContent,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            tasksApi.clear("SOME_ID")

            assertEquals(1, mockEngine.requestHistory.size)
            val request = mockEngine.requestHistory.first()
            val queryParams = request.url.parameters
            assertEquals("/tasks/v1/lists/SOME_ID/clear", request.url.encodedPath)
            assertEquals(0, queryParams.names().size)
            assertEquals(HttpMethod.Post, request.method)
        }
    }

    @Test
    fun `TasksApi clear failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTasksApi(mockEngine) { tasksApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    tasksApi.clear("")
                }
            }
        }

        assertEquals(1, mockEngine.responseHistory.size)
        assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
    }
}