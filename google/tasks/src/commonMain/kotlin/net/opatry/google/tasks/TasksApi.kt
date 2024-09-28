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

package net.opatry.google.tasks

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
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
import kotlinx.datetime.Instant
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TasksListResponse

/**
 * Service for interacting with the [Google Tasks REST API](https://developers.google.com/tasks/reference/rest/v1/tasks).
 */
class TasksApi(
    private val httpClient: HttpClient
) {
    /**
     * [Clears all completed tasks](https://developers.google.com/tasks/reference/rest/v1/tasks/clear) from the specified task list. The affected tasks will be marked as 'hidden' and no longer be returned by default when retrieving all tasks for a task list.
     *
     * @param taskListId Task list identifier.
     */
    suspend fun clear(taskListId: String) {
        val response = httpClient.post("tasks/v1/lists/${taskListId}/clear")

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Deletes the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/delete) from the task list. If the task is assigned, both the assigned task and the original task (in Docs, Chat Spaces) are deleted. To delete the assigned task only, navigate to the assignment surface and unassign the task from there.
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     */
    suspend fun delete(taskListId: String, taskId: String) {
        val response = httpClient.delete("tasks/v1/lists/${taskListId}/tasks/${taskId}")

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Returns the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/get).
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     *
     * @return an instance of [Task].
     */
    suspend fun get(taskListId: String, taskId: String): Task {
        val response = httpClient.get("tasks/v1/lists/${taskListId}/tasks/${taskId}") {
            contentType(ContentType.Application.Json)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Creates a new task](https://developers.google.com/tasks/reference/rest/v1/tasks/insert) on the specified task list. Tasks assigned from Docs or Chat Spaces cannot be inserted from Tasks Public API; they can only be created by assigning them from Docs or Chat Spaces. A user can have up to 20,000 non-hidden tasks per list and up to 100,000 tasks in total at a time.
     *
     * @param taskListId Task list identifier.
     * @param task the task data to insert.
     * @param parentTaskId Parent task identifier. If the task is created at the top level, this parameter is omitted. An assigned task cannot be a parent task, nor can it have a parent. Setting the parent to an assigned task results in failure of the request.
     * @param previousTaskId Previous sibling task identifier. If the task is created at the first position among its siblings, this parameter is omitted.
     *
     * @return a newly created instance of [Task].
     */
    suspend fun insert(taskListId: String, task: Task, parentTaskId: String? = null, previousTaskId: String? = null): Task {
        val response = httpClient.post("tasks/v1/lists/${taskListId}/tasks") {
            if (parentTaskId != null) {
                parameter("parent", parentTaskId)
            }
            if (previousTaskId != null) {
                parameter("previous", previousTaskId)
            }
            contentType(ContentType.Application.Json)
            setBody(task)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * Returns [all tasks in the specified task list](https://developers.google.com/tasks/reference/rest/v1/tasks/list). Does not return assigned tasks be default (from Docs, Chat Spaces). A user can have up to 20,000 non-hidden tasks per list and up to 100,000 tasks in total at a time.
     *
     * @param taskListId Task list identifier.
     * @param completedMin Lower bound for a task's completion date (as an RFC 3339 timestamp) to filter by. The default is not to filter by completion date.
     * @param completedMax Upper bound for a task's completion date (as an RFC 3339 timestamp) to filter by. The default is not to filter by completion date.
     * @param dueMin Lower bound for a task's due date (as an RFC 3339 timestamp) to filter by. The default is not to filter by due date.
     * @param dueMax Upper bound for a task's due date (as an RFC 3339 timestamp) to filter by. The default is not to filter by due date.
     * @param maxResults Maximum number of tasks returned on one page. The default is 20 (max allowed: 100).
     * @param pageToken Token specifying the result page to return.
     * @param showCompleted Flag indicating whether completed tasks are returned in the result. Note that [showHidden] must also be `true` to show tasks completed in first party clients, such as the web UI and Google's mobile apps. The default is `true`.
     * @param showDeleted Flag indicating whether deleted tasks are returned in the result. The default is `false`.
     * @param showHidden Flag indicating whether hidden tasks are returned in the result. The default is `false`.
     * @param updatedMin Lower bound for a task's last modification time (as an RFC 3339 timestamp) to filter by. The default is not to filter by last modification time.
     * @param showAssigned Flag indicating whether tasks assigned to the current user are returned in the result. The default is `false`.
     *
     * @return an instance of [TasksListResponse].
     */
    suspend fun list(
        taskListId: String,
        completedMin: Instant? = null,
        completedMax: Instant? = null,
        dueMin: Instant? = null,
        dueMax: Instant? = null,
        maxResults: Int = 20,
        pageToken: String? = null,
        showCompleted: Boolean = true,
        showDeleted: Boolean = false,
        showHidden: Boolean = false,
        updatedMin: Instant? = null,
        showAssigned: Boolean = false
        ): TasksListResponse {
        val response = httpClient.get("tasks/v1/lists/${taskListId}/tasks") {
            if (completedMin != null) {
                parameter("completedMin", completedMin.toString())
            }
            if (completedMax != null) {
                parameter("completedMax", completedMax.toString())
            }
            if (dueMin != null) {
                parameter("dueMin", dueMin.toString())
            }
            if (dueMax != null) {
                parameter("dueMax", dueMax.toString())
            }
            parameter("maxResults", maxResults.toString())
            if (pageToken != null) {
                parameter("pageToken", pageToken)
            }
            parameter("showCompleted", showCompleted.toString())
            parameter("showDeleted", showDeleted.toString())
            parameter("showHidden", showHidden.toString())
            if (updatedMin != null) {
                parameter("updatedMin", updatedMin.toString())
            }
            parameter("showAssigned", showAssigned.toString())
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * Iterate on [list]'s paginated results and returns single list of all tasks.
     *
     * @see [list]
     */
    suspend fun listAll(
        taskListId: String,
        completedMin: Instant? = null,
        completedMax: Instant? = null,
        dueMin: Instant? = null,
        dueMax: Instant? = null,
        showCompleted: Boolean = true,
        showDeleted: Boolean = false,
        showHidden: Boolean = false,
        updatedMin: Instant? = null,
        showAssigned: Boolean = false
    ): List<Task> {
        var nextPageToken: String? = null
        return buildList {
            do {
                val response = list(
                    taskListId,
                    completedMin,
                    completedMax,
                    dueMin,
                    dueMax,
                    maxResults = 100,
                    nextPageToken,
                    showCompleted,
                    showDeleted,
                    showHidden,
                    updatedMin,
                    showAssigned
                )
                addAll(response.items)
                nextPageToken = response.nextPageToken
            } while (nextPageToken != null)
        }
    }

    /**
     * [Moves the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/move) to another position in the destination task list.
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     * @param parentTaskId New parent task identifier. If the task is moved to the top level, this parameter is omitted. Assigned tasks can not be set as parent task (have subtasks) or be moved under a parent task (become subtasks).
     * @param previousTaskId New previous sibling task identifier. If the task is moved to the first position among its siblings, this parameter is omitted.
     * @param destinationTaskListId Destination task list identifier. If set, the task is moved from [taskListId] to the [destinationTaskListId] list. Otherwise, the task is moved within its current list. Recurrent tasks cannot currently be moved between lists.
     *
     * @return an instance of [Task].
     */
    suspend fun move(taskListId: String, taskId: String, parentTaskId: String? = null, previousTaskId: String? = null, destinationTaskListId: String? = null): Task {
        val response = httpClient.post("tasks/v1/lists/${taskListId}/tasks/${taskId}/move") {
            if (parentTaskId != null) {
                parameter("parent", parentTaskId)
            }
            if (previousTaskId != null) {
                parameter("previous", previousTaskId)
            }
            if (destinationTaskListId != null) {
                @Suppress("SpellCheckingInspection")
                parameter("destinationTasklist", destinationTaskListId)
            }
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Updates the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/patch). This method supports patch semantics.
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     * @param task the task data to patch.
     * 
     * @return an instance of [Task].
     */
    suspend fun patch(taskListId: String, taskId: String, task: Task): Task {
        val response = httpClient.patch("tasks/v1/lists/${taskListId}/tasks/${taskId}") {
            contentType(ContentType.Application.Json)
            setBody(task)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }

    /**
     * [Updates the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/update).
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     * @param task the task data to update.
     *
     * @return an instance of [Task].
     */
    suspend fun update(taskListId: String, taskId: String, task: Task): Task {
        val response = httpClient.put("tasks/v1/lists/${taskListId}/tasks/${taskId}") {
            contentType(ContentType.Application.Json)
            setBody(task)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }
}