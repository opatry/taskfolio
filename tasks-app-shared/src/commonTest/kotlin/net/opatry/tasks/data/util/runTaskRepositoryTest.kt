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

package net.opatry.tasks.data.util

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.ErrorResponse
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.TasksAppDatabase


@Suppress("TestFunctionName")
fun NoContentMockEngine() = MockEngine {
    respond("", HttpStatusCode.NoContent)
}

@Suppress("TestFunctionName")
fun ErrorMockEngine(code: HttpStatusCode) = MockEngine {
    val errorResponse = ErrorResponse(
        ErrorResponse.Error(
            code.value,
            message = code.description,
            errors = listOf(
                ErrorResponse.Error.ErrorDetail(
                    message = code.description,
                    domain = "global",
                    reason = "backendError",
                )
            )
        )
    )
    respond(
        Json.encodeToString(errorResponse),
        code,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}

internal fun runTaskRepositoryTest(
    mockEngine: MockEngine = NoContentMockEngine(),
    test: suspend TestScope.(TaskRepository) -> Unit
) = runTest {
    val db = Room.inMemoryDatabaseBuilder<TasksAppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(backgroundScope.coroutineContext)
        .build()
    val taskListDao = db.getTaskListDao()
    val taskDao = db.getTaskDao()

    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json()
        }
    }
    val taskListsApi = TaskListsApi(httpClient)
    val tasksApi = TasksApi(httpClient)
    val repository = TaskRepository(taskListDao, taskDao, taskListsApi, tasksApi)

    try {
        test(repository)
    } finally {
        db.close()
    }
}
