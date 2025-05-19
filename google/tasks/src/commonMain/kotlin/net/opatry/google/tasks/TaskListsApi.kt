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

package net.opatry.google.tasks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.compression.compress
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList

/**
 * Service for interacting with the [Google Task Lists REST API](https://developers.google.com/tasks/reference/rest/v1/tasklists).
 */
interface TaskListsApi {
    /**
     * [Deletes the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/delete). If the list contains assigned tasks, both the assigned tasks and the original tasks in the assignment surface (Docs, Chat Spaces) are deleted.
     *
     * @param taskListId Task list identifier.
     */
    suspend fun delete(taskListId: String)

    /**
     * [Returns the authenticated user's default task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/get).
     * It uses the special `taskListId` value `@default`.
     *
     * @return the instance of [TaskList] of the default task list.
     */
    suspend fun default(): TaskList

    /**
     * [Returns the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/get).
     *
     * @param taskListId Task list identifier.
     *
     * @return an instance of [TaskList].
     */
    suspend fun get(taskListId: String): TaskList

    /**
     * [Creates a new task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/insert) and adds it to the authenticated user's task lists. A user can have up to 2000 lists at a time.
     *
     * @param taskList  the task list to insert.
     *
     * @return a newly created instance of [TaskList].
     */
    suspend fun insert(taskList: TaskList): TaskList

    /**
     * [Returns all the authenticated user's task lists](https://developers.google.com/tasks/reference/rest/v1/tasklists/list). A user can have up to 2000 lists at a time.
     *
     * @param maxResults Maximum number of task lists returned on one page. Optional. The default is 20 (max allowed: 100).
     * @param pageToken Token specifying the result page to return. Optional.
     *
     * @return an instance of [ResourceListResponse] of type [TaskList], whose type is always [ResourceType.TaskLists].
     */
    suspend fun list(maxResults: Int = 20, pageToken: String? = null): ResourceListResponse<TaskList>

    /**
     * [Updates the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/patch). This method supports patch semantics.
     *
     * @param taskListId Task list identifier.
     * @param taskList  the task list to patch.
     *
     * @return an instance of [TaskList].
     */
    suspend fun patch(taskListId: String, taskList: TaskList): TaskList

    /**
     * [Updates the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/update).
     *
     * @param taskListId Task list identifier.
     * @param taskList  the task list to update.
     *
     * @return an instance of [TaskList].
     */
    suspend fun update(taskListId: String, taskList: TaskList): TaskList
}

class HttpTaskListsApi(
    private val httpClient: HttpClient
) : TaskListsApi {
    override suspend fun delete(taskListId: String) {
        val response = httpClient.delete("tasks/v1/users/@me/lists/${taskListId}")

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    override suspend fun default() = get("@default")

    override suspend fun get(taskListId: String): TaskList {
        val response = httpClient.get("tasks/v1/users/@me/lists/${taskListId}")

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    override suspend fun insert(taskList: TaskList): TaskList {
        val response = httpClient.post("tasks/v1/users/@me/lists") {
            contentType(ContentType.Application.Json)
            compress("gzip")
            setBody(taskList)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    override suspend fun list(maxResults: Int, pageToken: String?): ResourceListResponse<TaskList> {
        val response = httpClient.get("tasks/v1/users/@me/lists") {
            parameter("maxResults", maxResults.coerceIn(0, 100))
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
        }
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    override suspend fun patch(taskListId: String, taskList: TaskList): TaskList {
        val response = httpClient.patch("tasks/v1/users/@me/lists/${taskListId}") {
            contentType(ContentType.Application.Json)
            setBody(taskList)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    override suspend fun update(taskListId: String, taskList: TaskList): TaskList {
        val response = httpClient.put("tasks/v1/users/@me/lists/${taskListId}") {
            contentType(ContentType.Application.Json)
            setBody(taskList)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }
}

/**
 * Iterate on [TaskListsApi.list]'s paginated results and returns single list of all task lists.
 *
 * @see [TaskListsApi.list]
 */
suspend fun TaskListsApi.listAll(): List<TaskList> {
    var nextPageToken: String? = null
    return buildList {
        do {
            val response = list(maxResults = 100, nextPageToken)
            addAll(response.items)
            nextPageToken = response.nextPageToken
        } while (!nextPageToken.isNullOrEmpty())
    }
}
