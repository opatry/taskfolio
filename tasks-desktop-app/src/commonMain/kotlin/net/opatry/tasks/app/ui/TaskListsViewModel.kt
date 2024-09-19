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
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList

class TaskRepository(
    private val taskListsApi: TaskListsApi,
    private val tasksApi: TasksApi,
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
    val taskLists: StateFlow<List<TaskList>>
            field = MutableStateFlow<List<TaskList>>(emptyList())

    init {
        // cold flow?
        viewModelScope.launch {
            // TODO flow?
            taskLists.value = taskRepository.getTaskLists()
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
}
