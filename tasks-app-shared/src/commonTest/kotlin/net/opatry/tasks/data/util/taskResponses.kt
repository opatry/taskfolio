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

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import java.net.ConnectException


fun MockRequestHandleScope.respondNoContent(): HttpResponseData = respond("", HttpStatusCode.NoContent)
fun MockRequestHandleScope.respondNoNetwork(): HttpResponseData = throw ConnectException("No network")

fun MockRequestHandleScope.respondWithTaskLists(vararg idToTitles: Pair<String, String>): HttpResponseData {
    val taskLists = idToTitles.map { TaskList(id = it.first, title = it.second) }
    val taskListsResponse = ResourceListResponse(
        kind = ResourceType.TaskLists,
        etag = "\"ETAG_ETAG_ETAG\"",
        items = taskLists
    )
    return respond(
        content = Json.encodeToString(taskListsResponse),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}

fun MockRequestHandleScope.respondWithTaskList(id: String, title: String): HttpResponseData {
    return respond(
        content = Json.encodeToString(TaskList(id = id, title = title)),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}

fun MockRequestHandleScope.respondWithTasks(vararg idToTitles: Pair<String, String>): HttpResponseData {
    val tasks = idToTitles.map { Task(id = it.first, title = it.second) }
    val tasksResponse = ResourceListResponse(
        kind = ResourceType.Tasks,
        etag = "\"ETAG_ETAG_ETAG\"",
        items = tasks
    )
    return respond(
        content = Json.encodeToString(tasksResponse),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )
}