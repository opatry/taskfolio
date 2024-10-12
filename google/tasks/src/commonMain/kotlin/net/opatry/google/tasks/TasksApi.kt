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

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.datetime.Instant
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.Task

/**
 * Service for interacting with the [Google Tasks REST API](https://developers.google.com/tasks/reference/rest/v1/tasks).
 */
interface TasksApi {
    /**
     * [Clears all completed tasks](https://developers.google.com/tasks/reference/rest/v1/tasks/clear) from the specified task list. The affected tasks will be marked as 'hidden' and no longer be returned by default when retrieving all tasks for a task list.
     *
     * @param taskListId Task list identifier.
     */
    @POST("tasks/v1/lists/{taskListId}/clear")
    suspend fun clear(@Path("taskListId") taskListId: String)

    /**
     * [Deletes the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/delete) from the task list. If the task is assigned, both the assigned task and the original task (in Docs, Chat Spaces) are deleted. To delete the assigned task only, navigate to the assignment surface and unassign the task from there.
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     */
    @DELETE("tasks/v1/lists/{taskListId}/tasks/{taskId}")
    suspend fun delete(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String
    )

    /**
     * [Returns the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/get).
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     *
     * @return an instance of [Task].
     */
    @GET("tasks/v1/lists/{taskListId}/tasks/{taskId}")
    suspend fun get(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String
    ): Task

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
    @Headers(
        "Content-Type: application/json",
        "Content-Encoding: gzip",
    )
    @POST("tasks/v1/lists/{taskListId}/tasks")
    suspend fun insert(
        @Path("taskListId") taskListId: String,
        @Body task: Task,
        @Query("parent") parentTaskId: String? = null,
        @Query("previous") previousTaskId: String? = null
    ): Task

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
     * @return an instance of [ResourceListResponse] of type [Task], whose type is always [ResourceType.Tasks].
     */
    @GET("tasks/v1/lists/{taskListId}/tasks")
    suspend fun list(
        @Path("taskListId") taskListId: String,
        @Query("completedMin") completedMin: Instant? = null,
        @Query("completedMax") completedMax: Instant? = null,
        @Query("dueMin") dueMin: Instant? = null,
        @Query("dueMax") dueMax: Instant? = null,
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("showCompleted") showCompleted: Boolean = true,
        @Query("showDeleted") showDeleted: Boolean = false,
        @Query("showHidden") showHidden: Boolean = false,
        @Query("updatedMin") updatedMin: Instant? = null,
        @Query("showAssigned") showAssigned: Boolean = false
    ): ResourceListResponse<Task>

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
    @POST("tasks/v1/lists/{taskListId}/tasks/{taskId}/move")
    suspend fun move(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String,
        @Query("parent") parentTaskId: String? = null,
        @Query("previous") previousTaskId: String? = null,
        @Suppress("SpellCheckingInspection")
        @Query("destinationTasklist") destinationTaskListId: String? = null
    ): Task

    /**
     * [Updates the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/patch). This method supports patch semantics.
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     * @param task the task data to patch.
     *
     * @return an instance of [Task].
     */
    @Headers("Content-Type: application/json")
    @PATCH("tasks/v1/lists/{taskListId}/tasks/{taskId}")
    suspend fun patch(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String,
        @Body task: Task
    ): Task

    /**
     * [Updates the specified task](https://developers.google.com/tasks/reference/rest/v1/tasks/update).
     *
     * @param taskListId Task list identifier.
     * @param taskId Task identifier.
     * @param task the task data to update.
     *
     * @return an instance of [Task].
     */
    @Headers("Content-Type: application/json")
    @PUT("tasks/v1/lists/{taskListId}/tasks/{taskId}")
    suspend fun update(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String,
        @Body task: Task
    ): Task
}

/**
 * Iterate on [TasksApi.list]'s paginated results and returns single list of all tasks.
 *
 * @see [TasksApi.list]
 */
suspend fun TasksApi.listAll(
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
        } while (!nextPageToken.isNullOrEmpty())
    }
}
