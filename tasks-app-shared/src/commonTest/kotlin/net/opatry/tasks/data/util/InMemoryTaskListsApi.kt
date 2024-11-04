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

import kotlinx.datetime.Clock
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicLong

class InMemoryTaskListsApi(vararg initialTaskLists: String) : TaskListsApi {
    private val taskListId = AtomicLong()
    private val storage = mutableMapOf<String, TaskList>()

    init {
        storage += initialTaskLists.map { title ->
            TaskList(
                kind = ResourceType.TaskList,
                id = taskListId.incrementAndGet().toString(),
                etag = "etag",
                title = title,
                updatedDate = Clock.System.now(),
                selfLink = "selfLink"
            )
        }.associateBy(TaskList::id)
    }

    var isNetworkAvailable = true
    val requests = mutableListOf<String>()
    val requestCount: Int
        get() = requests.size

    private fun <R> handleRequest(requestName: String, logic: () -> R): R {
        if (!isNetworkAvailable) throw ConnectException("Network unavailable")
        requests += requestName
        return logic()
    }

    override suspend fun delete(taskListId: String) {
        handleRequest("delete") {
            synchronized(this) {
                storage.remove(taskListId)
            }
        }
    }

    override suspend fun get(taskListId: String): TaskList {
        return handleRequest("get") {
            synchronized(this) {
                storage[taskListId] ?: error("Task list ($taskListId) not found")
            }
        }
    }

    override suspend fun insert(taskList: TaskList): TaskList {
        return handleRequest("insert") {
            val newTaskList = TaskList(
                kind = ResourceType.TaskList,
                id = taskListId.incrementAndGet().toString(),
                etag = "etag",
                title = taskList.title,
                updatedDate = Clock.System.now(),
                selfLink = "selfLink"
            )
            synchronized(this) {
                storage[newTaskList.id] = newTaskList
            }
            newTaskList
        }
    }

    override suspend fun list(maxResults: Int, pageToken: String?): ResourceListResponse<TaskList> {
        return handleRequest("list") {
            // TODO maxResults & token handling
            synchronized(this) {
                ResourceListResponse(
                    kind = ResourceType.TaskLists,
                    etag = "etag",
                    nextPageToken = null,
                    items = storage.values.toList()
                )
            }
        }
    }

    override suspend fun patch(taskListId: String, taskList: TaskList): TaskList {
        return update(taskListId, taskList)
    }

    override suspend fun update(taskListId: String, taskList: TaskList): TaskList {
        return handleRequest("update") {
            synchronized(this) {
                if (!storage.containsKey(taskListId)) {
                    error("Task list ($taskListId) not found")
                }
                val updatedTaskList = TaskList(
                    kind = ResourceType.TaskList,
                    id = taskListId,
                    etag = "etag",
                    title = taskList.title,
                    updatedDate = Clock.System.now(),
                    selfLink = "selfLink"
                )
                storage[taskListId] = updatedTaskList
                updatedTaskList
            }
        }
    }
}