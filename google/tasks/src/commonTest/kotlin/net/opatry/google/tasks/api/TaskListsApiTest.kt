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

package net.opatry.google.tasks.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList
import net.opatry.google.tasks.util.loadJson
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TaskListsApiTest {
    private fun runTaskListsApi(
        engine: HttpClientEngine,
        test: suspend TestScope.(api: TaskListsApi) -> Unit
    ) {
        HttpClient(engine) {
            install(ContentNegotiation) {
                json()
            }
        }.use { httpClient ->
            runTest {
                test(TaskListsApi(httpClient))
            }
        }
    }

    @Test
    fun `TaskListsApi list`() {
        MockEngine {
            respondJsonResource("/tasklists.json")
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                val taskLists = taskListsApi.list()

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists", request.url.encodedPath)
                assertEquals(1, queryParams.names().size)
                assertEquals("20", queryParams["maxResults"])
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(ResourceType.TaskLists, taskLists.kind)
                assertEquals(2, taskLists.items.size)
                assertContentEquals(listOf("My tasks", "Other tasks"), taskLists.items.map(TaskList::title))
            }
        }
    }

    @Test
    fun `TaskListsApi list failure`() {
        MockEngine {
            respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json"))
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.list()
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `TaskListsApi insert`() {
        MockEngine {
            respondJsonResource("/tasklist.json")
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                val taskList = taskListsApi.insert(TaskList("Other tasks"))

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists", request.url.encodedPath)
                assertEquals(0, queryParams.names().size)
                assertEquals(HttpMethod.Post, request.method)
                assertEquals(ResourceType.TaskList, taskList.kind)
                assertEquals("Other tasks", taskList.title)
                assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
            }
        }
    }

    @Test
    fun `TaskListsApi insert failure`() {
        MockEngine {
            respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json"))
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.insert(TaskList(""))
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `TaskListsApi get`() {
        MockEngine {
            respondJsonResource("/tasklist.json")
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                val taskList = taskListsApi.get("OXl0d1JibXgyeW1zWWFIMw")

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request.url.encodedPath)
                assertEquals(0, queryParams.names().size)
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(ResourceType.TaskList, taskList.kind)
                assertEquals("Other tasks", taskList.title)
                assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
            }
        }
    }

    @Test
    fun `TaskListsApi get failure`() {
        MockEngine {
            respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json"))
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.get("")
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `TaskListsApi delete`() {
        MockEngine {
            respond("", HttpStatusCode.NoContent)
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                taskListsApi.delete("OXl0d1JibXgyeW1zWWFIMw")

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request.url.encodedPath)
                assertEquals(0, queryParams.names().size)
                assertEquals(HttpMethod.Delete, request.method)
            }
        }
    }

    @Test
    fun `TaskListsApi delete failure`() {
        MockEngine {
            respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json"))
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.delete("")
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `TaskListsApi patch`() {
        MockEngine {
            respondJsonResource("/tasklist.json")
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                val taskList = taskListsApi.patch("OXl0d1JibXgyeW1zWWFIMw", TaskList("Other tasks"))

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request.url.encodedPath)
                assertEquals(0, queryParams.names().size)
                assertEquals(HttpMethod.Patch, request.method)
                assertEquals(ResourceType.TaskList, taskList.kind)
                assertEquals("Other tasks", taskList.title)
                assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
            }
        }
    }

    @Test
    fun `TaskListsApi patch failure`() {
        MockEngine {
            respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json"))
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.patch("", TaskList(""))
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }

    @Test
    fun `TaskListsApi update`() {
        MockEngine {
            respondJsonResource("/tasklist.json")
        }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                val taskList = taskListsApi.update("OXl0d1JibXgyeW1zWWFIMw", TaskList("Other tasks"))

                assertEquals(1, mockEngine.requestHistory.size)
                val request = mockEngine.requestHistory.first()
                val queryParams = request.url.parameters
                assertEquals("/tasks/v1/users/@me/lists/OXl0d1JibXgyeW1zWWFIMw", request.url.encodedPath)
                assertEquals(0, queryParams.names().size)
                assertEquals(HttpMethod.Put, request.method)
                assertEquals(ResourceType.TaskList, taskList.kind)
                assertEquals("Other tasks", taskList.title)
                assertEquals("OXl0d1JibXgyeW1zWWFIMw", taskList.id)
            }
        }
    }

    @Test
    fun `TaskListsApi update failure`() {
        MockEngine { respondError(HttpStatusCode.BadRequest, loadJson("/error_400.json")) }.use { mockEngine ->
            runTaskListsApi(mockEngine) { taskListsApi ->
                assertThrows(ClientRequestException::class.java) {
                    runBlocking {
                        taskListsApi.update("", TaskList(""))
                    }
                }
            }

            assertEquals(1, mockEngine.responseHistory.size)
            assertEquals(HttpStatusCode.BadRequest, mockEngine.responseHistory.first().statusCode)
        }
    }
}