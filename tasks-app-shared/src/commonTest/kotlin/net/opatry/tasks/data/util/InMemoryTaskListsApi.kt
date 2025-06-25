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

package net.opatry.tasks.data.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.TaskList
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class)
class InMemoryTaskListsApi(vararg initialTaskLists: String) : TaskListsApi {
    private val taskListId = AtomicLong(0)
    private val storage = mutableMapOf<String, TaskList>()

    private val mutex = Mutex()

    init {
        storage += initialTaskLists.map { title ->
            TaskList(
                kind = ResourceType.TaskList,
                id = taskListId.addAndFetch(1).toString(),
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

    private suspend fun <R> handleRequest(requestName: String, logic: suspend () -> R): R {
        if (!isNetworkAvailable) throw IOException("Network unavailable")
        requests += requestName
        return logic()
    }

    override suspend fun delete(taskListId: String) {
        handleRequest("delete") {
            mutex.withLock {
                storage.remove(taskListId)
            }
        }
    }

    override suspend fun default(): TaskList {
        return handleRequest("default") {
            mutex.withLock {
                storage["1"] ?: error("Task list (@default / 1) not found")
            }
        }
    }

    override suspend fun get(taskListId: String): TaskList {
        return handleRequest("get") {
            mutex.withLock {
                storage[taskListId] ?: error("Task list ($taskListId) not found")
            }
        }
    }

    override suspend fun insert(taskList: TaskList): TaskList {
        return handleRequest("insert") {
            val newTaskList = TaskList(
                kind = ResourceType.TaskList,
                id = taskListId.addAndFetch(1).toString(),
                etag = "etag",
                title = taskList.title,
                updatedDate = Clock.System.now(),
                selfLink = "selfLink"
            )
            mutex.withLock {
                storage[newTaskList.id] = newTaskList
            }
            newTaskList
        }
    }

    override suspend fun list(maxResults: Int, pageToken: String?): ResourceListResponse<TaskList> {
        return handleRequest("list") {
            // TODO maxResults & token handling
            mutex.withLock {
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
            mutex.withLock {
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