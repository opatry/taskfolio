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
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList

/**
 * Service for interacting with the [Google Task Lists REST API](https://developers.google.com/tasks/reference/rest/v1/tasklists).
 */
interface TaskListsApi {
    @DELETE("tasks/v1/users/@me/lists/{taskListId}")
    suspend fun delete(@Path("taskListId") taskListId: String)

    /**
     * [Returns the authenticated user's default task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/get).
     * It uses the special `taskListId` value `@default`.
     *
     * @return the instance of [TaskList] of the default task list.
     */
    suspend fun default() = get("@default")

    /**
     * [Returns the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/get).
     *
     * @param taskListId Task list identifier.
     *
     * @return an instance of [TaskList].
     */
    @GET("tasks/v1/users/@me/lists/{taskListId}")
    suspend fun get(@Path("taskListId") taskListId: String): TaskList

    /**
     * [Creates a new task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/insert) and adds it to the authenticated user's task lists. A user can have up to 2000 lists at a time.
     *
     * @param taskList  the task list to insert.
     *
     * @return a newly created instance of [TaskList].
     */
    @Headers(
        "Content-Type: application/json",
        "Content-Encoding: gzip",
    )
    @POST("tasks/v1/users/@me/lists")
    suspend fun insert(@Body taskList: TaskList): TaskList

    /**
     * [Returns all the authenticated user's task lists](https://developers.google.com/tasks/reference/rest/v1/tasklists/list). A user can have up to 2000 lists at a time.
     *
     * @param maxResults Maximum number of task lists returned on one page. Optional. The default is 20 (max allowed: 100).
     * @param pageToken Token specifying the result page to return. Optional.
     *
     * @return an instance of [ResourceListResponse] of type [TaskList], whose type is always [ResourceType.TaskLists].
     */
    @GET("tasks/v1/users/@me/lists")
    suspend fun list(
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null
    ): ResourceListResponse<TaskList>

    /**
     * [Updates the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/patch). This method supports patch semantics.
     *
     * @param taskListId Task list identifier.
     * @param taskList  the task list to patch.
     *
     * @return an instance of [TaskList].
     */
    @Headers("Content-Type: application/json")
    @PATCH("tasks/v1/users/@me/lists/{taskListId}")
    suspend fun patch(
        @Path("taskListId") taskListId: String,
        @Body taskList: TaskList
    ): TaskList

    /**
     * [Updates the authenticated user's specified task list](https://developers.google.com/tasks/reference/rest/v1/tasklists/update).
     *
     * @param taskListId Task list identifier.
     * @param taskList  the task list to update.
     *
     * @return an instance of [TaskList].
     */
    @Headers("Content-Type: application/json")
    @PUT("tasks/v1/users/@me/lists/{taskListId}")
    suspend fun update(
        @Path("taskListId") taskListId: String,
        @Body taskList: TaskList
    ): TaskList
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
