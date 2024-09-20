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

package net.opatry.tasks.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class TaskRepository(
    /*private*/ val taskListsApi: TaskListsApi,
    /*private*/ val tasksApi: TasksApi,
    // TODO database
) {
    suspend fun getTaskLists(): List<TaskList> {
        return withContext(Dispatchers.IO) {
            taskListsApi.listAll()
        }
    }

    suspend fun createTaskList(title: String): TaskList {
        return withContext(Dispatchers.IO) {
            taskListsApi.insert(TaskList(title = title))
        }
    }

    suspend fun createTask(taskList: TaskList, title: String, dueDate: Instant? = null): Task {
        return withContext(Dispatchers.IO) {
            tasksApi.insert(
                taskList.id,
                Task(
                    title = title,
                    dueDate = dueDate
                )
            )
        }
    }
}

class TaskListsViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val _taskLists = MutableStateFlow<List<TaskList>>(emptyList())
    val taskLists: StateFlow<List<TaskList>>
        get() = _taskLists

    init {
        // cold flow?
        viewModelScope.launch {
            // TODO flow?
            _taskLists.value = try {
                taskRepository.getTaskLists()
            } catch (e: Exception) {
                println("Error fetching task lists: $e")
                // TODO error handling
                emptyList()
            }
        }
    }

    fun createTaskList(title: String) {
        viewModelScope.launch {
            try {
                taskRepository.createTaskList(title)
            } catch (e: Exception) {
                println("Error creating task list: $e")
                // TODO error handling
            }
        }
    }

    // TODO when "delete task" (or list) is done, manage a "Undo" snackbar
    //  - either apply it remotely and on undo, do another request to restore through API
    //  - or "hide" locally, on undo "un-hide", on dismiss, apply remotely

    fun createTask(taskList: TaskList, title: String, dueDate: Instant? = null) {
        viewModelScope.launch {
            try {
                taskRepository.createTask(taskList, title, dueDate)
            } catch (e: Exception) {
                println("Error creating task: $e")
                // TODO error handling
            }
        }
    }

    fun fetch() {
        viewModelScope.launch {
            refresh()
        }
    }

    private suspend fun refresh() {
        _taskLists.value = try {
            taskRepository.getTaskLists().onEach { l ->
                println("${l.id}: ${l.etag}")
            }
        } catch (e: Exception) {
            println("Error fetching task lists: $e")
            emptyList()
        }
    }

    fun updateTitleList(taskList: TaskList) {
        viewModelScope.launch {
            taskRepository.taskListsApi.update(taskList.id, taskList.copy(title = "Dev tasks list ${System.currentTimeMillis()}"))
            refresh()
        }
    }

    fun removeRandomTask(taskList: TaskList) {
        viewModelScope.launch {
            val children = taskRepository.tasksApi.listAll(taskList.id, showHidden = true, showDeleted = true)
            children.randomOrNull()?.let {
                println("BEFORE: " + children.joinToString(", ") { t -> "${t.id}  ${t.isDeleted}" })
                taskRepository.tasksApi.delete(taskList.id, it.id)
                val children2 = taskRepository.tasksApi.listAll(taskList.id, showHidden = true, showDeleted = true)
                println("AFTER : " + children2.joinToString(", ") { t -> "${t.id}  ${t.isDeleted}" })
                refresh()
            }
        }
    }

    fun addRandomTask(taskList: TaskList) {
        viewModelScope.launch {
            taskRepository.tasksApi.insert(taskList.id, Task(title = "Toto"))
            refresh()
        }
    }

    fun updateRandomTask(taskList: TaskList) {
        viewModelScope.launch {
            val children = taskRepository.tasksApi.listAll(taskList.id)
            children.randomOrNull()?.let {
                taskRepository.tasksApi.update(taskList.id, it.id, it.copy(title = "Updated (${System.currentTimeMillis()})"))
                refresh()
            }
        }
    }

    fun updateRandomTaskDueDate(taskList: TaskList) {
        viewModelScope.launch {
            val children = taskRepository.tasksApi.listAll(taskList.id)
            children.randomOrNull()?.let {
                println("BEFORE: " + children.joinToString(", ") { t -> "${t.id}  ${t.dueDate}" })
                taskRepository.tasksApi.update(
                    taskList.id,
                    it.id,
                    it.copy(dueDate = (it.dueDate ?: Clock.System.now()) + Random.nextInt(3).days + Random.nextInt(8, 20).hours)
                )
                val children2 = taskRepository.tasksApi.listAll(taskList.id, showDeleted = false)
                println("AFTER : " + children2.joinToString(", ") { t -> "${t.id}  ${t.dueDate}" })
                refresh()
            }
        }
    }
}
