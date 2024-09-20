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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel

private fun TaskListDataModel.asTaskListUIModel(): TaskListUIModel {
    // TODO children
    // TODO date formatter
    return TaskListUIModel(
        id = id,
        title = title,
        lastUpdate = lastUpdate.toString(),
        tasks = tasks.map(TaskDataModel::asTaskUIModel)
    )
}

private fun TaskDataModel.asTaskUIModel(): TaskUIModel {
    // TODO date formatter
    return TaskUIModel(
        id = id,
        title = title,
        dueDate = dueDate?.toString() ?: "",
        isCompleted = false
    )
}

class TaskListsViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    // FIXME won't work for empty list
    @OptIn(ExperimentalCoroutinesApi::class)
    val taskLists: Flow<List<TaskListUIModel>> = taskRepository.getTaskLists().mapLatest { allLists ->
        allLists.map(TaskListDataModel::asTaskListUIModel)
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    init {
        // cold flow?
        viewModelScope.launch {
            try {
                taskRepository.fetchTaskLists()
            } catch (e: Exception) {
                // most likely no network
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

    fun createTask(taskList: TaskListUIModel, title: String, dueDate: Instant? = null) {
        viewModelScope.launch {
            try {
                taskRepository.createTask(taskList.id, title, dueDate)
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
        try {
            taskRepository.fetchTaskLists()
        } catch (e: Exception) {
            // most likely no network
        }
    }
}
