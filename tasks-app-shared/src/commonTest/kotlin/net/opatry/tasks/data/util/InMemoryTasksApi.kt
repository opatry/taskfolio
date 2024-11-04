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

package net.opatry.tasks

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.google.tasks.model.ResourceType
import net.opatry.google.tasks.model.Task
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicLong

class InMemoryTasksApi(
    vararg initialTasks: Pair<String, List<String>>
) : TasksApi {
    private val taskId = AtomicLong()
    private val storage = mutableMapOf<String, MutableList<Task>>()

    init {
        storage += initialTasks.associate { (taskListId, titles) ->
            taskListId to titles.map { Task(id = taskId.incrementAndGet().toString(), title = it) }.toMutableList()
        }
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

    override suspend fun clear(taskListId: String) {
        handleRequest("clear") {
            synchronized(this) {
                val tasks = storage[taskListId] ?: error("Task list ($taskListId) not found")
                if (tasks.isEmpty()) {
                    return@handleRequest
                }
                tasks.map { task ->
                    if (task.isCompleted) {
                        task.copy(isHidden = true)
                    } else {
                        task
                    }
                }
                storage[taskListId] = tasks
            }
        }
    }

    override suspend fun delete(taskListId: String, taskId: String) {
        handleRequest("delete") {
            synchronized(this) {
                val tasks = storage[taskListId] ?: error("Task list ($taskListId) not found")
                if (tasks.none { it.id == taskId }) {
                    error("Task ($taskId) not found in task list ($taskListId)")
                }
                tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(isDeleted = true, isHidden = true)
                    } else {
                        task
                    }
                }
                storage[taskListId] = tasks
            }
        }
    }

    override suspend fun get(taskListId: String, taskId: String): Task {
        return handleRequest("get") {
            val tasks = synchronized(this) {
                storage[taskListId] ?: error("Task list ($taskListId) not found")
            }
            tasks.find { task -> task.id == taskId } ?: error("Task ($taskId) not found in task list ($taskListId)")
        }
    }

    override suspend fun insert(taskListId: String, task: Task, parentTaskId: String?, previousTaskId: String?): Task {
        return handleRequest("insert") {
            val newTask = task.copy(
                id = taskId.incrementAndGet().toString(),
                etag = "etag",
                title = task.title,
                updatedDate = Clock.System.now(),
                selfLink = "selfLink",
                parent = parentTaskId,
                position = "00000000000000000000", // TODO compute position from previous
            )
            synchronized(this) {
                val tasks = storage.getOrDefault(taskListId, mutableListOf())
                val previousTaskIndex = tasks.indexOfFirst { it.id == previousTaskId }.coerceAtLeast(0)
                tasks.add(previousTaskIndex, newTask)
                storage[taskListId] = tasks
            }
            newTask
        }
    }

    override suspend fun list(
        taskListId: String,
        completedMin: Instant?,
        completedMax: Instant?,
        dueMin: Instant?,
        dueMax: Instant?,
        maxResults: Int,
        pageToken: String?,
        showCompleted: Boolean,
        showDeleted: Boolean,
        showHidden: Boolean,
        updatedMin: Instant?,
        showAssigned: Boolean
    ): ResourceListResponse<Task> {
        return handleRequest("list") {
            // TODO maxResults & token handling
            val tasks = synchronized(this) {
                storage[taskListId] ?: error("Task list ($taskListId) not found")
            }
            val filteredTasks = tasks.filter { task ->
                // Check if completed tasks should be shown
                val completed = task.completedDate
                if (!showCompleted && completed != null) {
                    return@filter false
                }
                // Check completion date range
                if (completed != null) {
                    if (completedMin != null && completed < completedMin) {
                        return@filter false
                    }
                    if (completedMax != null && completed > completedMax) {
                        return@filter false
                    }
                }

                // Check due date range
                val due = task.dueDate
                if (due != null) {
                    if (dueMin != null && due < dueMin) {
                        return@filter false
                    }
                    if (dueMax != null && due > dueMax) {
                        return@filter false
                    }
                }

                // Check last updated date
                val updated = task.updatedDate
                if (updatedMin != null && updated < updatedMin) {
                    return@filter false
                }

                // Check if deleted tasks should be shown
                if (!showDeleted && task.isDeleted) {
                    return@filter false
                }

                // Check if hidden tasks should be shown
                if (!showHidden && task.isHidden) {
                    return@filter false
                }

                // Check if assigned tasks should be shown
                if (!showAssigned && task.assignmentInfo != null) {
                    return@filter false
                }
                true
            }
            // TODO pagination
            ResourceListResponse(ResourceType.Tasks, "etag", null, filteredTasks)
        }
    }

    override suspend fun move(
        taskListId: String,
        taskId: String,
        parentTaskId: String?,
        previousTaskId: String?,
        destinationTaskListId: String?
    ): Task {
        return handleRequest("move") {
            synchronized(this) {
                val tasks = storage[taskListId] ?: error("Task list ($taskListId) not found")
                val task = tasks.find { task -> task.id == taskId } ?: error("Task ($taskId) not found in task list ($taskListId)")
                val targetListId = destinationTaskListId ?: taskListId
                val destinationTasks = if (taskListId != targetListId) {
                    storage[targetListId] ?: error("Task list ($targetListId) not found")
                } else tasks

                if (parentTaskId != null && destinationTasks.none { it.id == parentTaskId }) {
                    error("Task ($parentTaskId) not found in task list ($targetListId)")
                }
                val previousTaskIndex = destinationTasks.indexOfFirst { it.id == previousTaskId }
                if (previousTaskIndex == -1) {
                    error("Task ($previousTaskId) not found in task list ($targetListId)")
                }
                // TODO "position"
                val moved = task.copy(parent = parentTaskId)
                destinationTasks.add(previousTaskIndex + 1, moved)
                storage[targetListId] = destinationTasks
                if (taskListId != targetListId) {
                    storage[taskListId] = tasks.filter { it.id != taskId }.toMutableList()
                }
                moved
            }
        }
    }

    override suspend fun patch(taskListId: String, taskId: String, task: Task): Task {
        return update(taskListId, taskId, task)
    }

    override suspend fun update(taskListId: String, taskId: String, task: Task): Task {
        return handleRequest("update") {
            synchronized(this) {
                val tasks = storage[taskListId] ?: error("Task list ($taskListId) not found")
                tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(updatedDate = Clock.System.now())
                    } else {
                        task
                    }
                }
                storage[taskListId] = tasks
                task
            }
        }
    }
}