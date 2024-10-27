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
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList
import net.opatry.google.tasks.util.loadJson
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TaskListsApiTest {

    @Test
    fun `TaskListsApi list`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(loadJson("/tasklists.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            val taskLists = taskListsApi.list()

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists", request?.url?.encodedPath)
            assertEquals(1, queryParams?.names()?.size)
            assertEquals("20", queryParams?.get("maxResults"))
            assertEquals(HttpMethod.Get, request?.method)
            assertEquals(ResourceType.TaskLists, taskLists.kind)
            assertEquals(2, taskLists.items.size)
            assertContentEquals(listOf("My tasks", "Other tasks"), taskLists.items.map(TaskList::title))
        }
    }

    @Test
    fun `TaskListsApi list failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.list()
                }
            }
        }
    }

    @Test
    fun `TaskListsApi insert`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(loadJson("/tasklist.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            val taskList = taskListsApi.insert(TaskList(title = "Other tasks"))

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists", request?.url?.encodedPath)
            assertEquals(0, queryParams?.names()?.size)
            assertEquals(HttpMethod.Post, request?.method)
            assertEquals(ResourceType.TaskList, taskList.kind)
            assertEquals("Other tasks", taskList.title)
            assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
        }
    }

    @Test
    fun `TaskListsApi insert failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.insert(TaskList(title = ""))
                }
            }
        }
    }

    @Test
    fun `TaskListsApi get`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(loadJson("/tasklist.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            val taskList = taskListsApi.get("OXl0d1JibXgyeW1zWWFIMw")

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request?.url?.encodedPath)
            assertEquals(0, queryParams?.names()?.size)
            assertEquals(HttpMethod.Get, request?.method)
            assertEquals(ResourceType.TaskList, taskList.kind)
            assertEquals("Other tasks", taskList.title)
            assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
        }
    }

    @Test
    fun `TaskListsApi get failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.get("")
                }
            }
        }
    }

    @Test
    fun `TaskListsApi delete`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.NoContent,
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            taskListsApi.delete("OXl0d1JibXgyeW1zWWFIMw")

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request?.url?.encodedPath)
            assertEquals(0, queryParams?.names()?.size)
            assertEquals(HttpMethod.Delete, request?.method)
        }
    }

    @Test
    fun `TaskListsApi delete failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.delete("")
                }
            }
        }
    }

    @Test
    fun `TaskListsApi patch`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(loadJson("/tasklist.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            val taskList = taskListsApi.patch("OXl0d1JibXgyeW1zWWFIMw", TaskList(title = "Other tasks"))

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request?.url?.encodedPath)
            assertEquals(0, queryParams?.names()?.size)
            assertEquals(HttpMethod.Patch, request?.method)
            assertEquals(ResourceType.TaskList, taskList.kind)
            assertEquals("Other tasks", taskList.title)
            assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
        }
    }

    @Test
    fun `TaskListsApi patch failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.patch("", TaskList(title = ""))
                }
            }
        }
    }

    @Test
    fun `TaskListsApi update`() {
        var request: HttpRequestData? = null
        val mockEngine = MockEngine {
            request = it
            respond(
                content = ByteReadChannel(loadJson("/tasklist.json")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            val taskList = taskListsApi.update("OXl0d1JibXgyeW1zWWFIMw", TaskList(title = "Other tasks"))

            val queryParams = request?.url?.parameters
            assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request?.url?.encodedPath)
            assertEquals(0, queryParams?.names()?.size)
            assertEquals(HttpMethod.Put, request?.method)
            assertEquals(ResourceType.TaskList, taskList.kind)
            assertEquals("Other tasks", taskList.title)
            assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
        }
    }

    @Test
    fun `TaskListsApi update failure`() {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(loadJson("/error_400.json")),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        usingTaskListsApi(mockEngine) { taskListsApi ->
            assertThrows(ClientRequestException::class.java) {
                runBlocking {
                    taskListsApi.update("", TaskList(title = ""))
                }
            }
        }
    }
}