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

package net.opatry.tasks.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.opatry.tasks.app.ui.model.DateRange
import net.opatry.tasks.app.ui.model.TaskId
import net.opatry.tasks.app.ui.model.TaskListId
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.app.ui.model.compareTo
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun TaskListDataModel.asTaskListUIModel(): TaskListUIModel {
    val (completedTasks, remainingTasks) = tasks.map(TaskDataModel::asTaskUIModel).partition(TaskUIModel::isCompleted)

    val taskGroups = when (sorting) {
        // no grouping
        TaskListSorting.Manual -> mapOf(null to remainingTasks)
        TaskListSorting.DueDate -> remainingTasks
            .sortedWith { o1, o2 -> o1.dateRange.compareTo(o2.dateRange) }
            .groupBy { task ->
                when (task.dateRange) {
                    // merge all overdue tasks to the same range
                    is DateRange.Overdue -> DateRange.Overdue(LocalDate.fromEpochDays(0), -1)
                    else -> task.dateRange
                }
            }
    }

    return TaskListUIModel(
        id = TaskListId(id),
        title = title,
        remainingTasks = taskGroups.toMap(),
        completedTasks = completedTasks,
        sorting = sorting,
    )
}

private fun TaskDataModel.asTaskUIModel(): TaskUIModel {
    return TaskUIModel(
        id = TaskId(id),
        title = title,
        notes = notes,
        dueDate = dueDate?.toLocalDateTime(TimeZone.currentSystemDefault())?.date,
        isCompleted = isCompleted,
        position = position,
        indent = indent,
    )
}

class TaskListsViewModel(
    private val taskRepository: TaskRepository,
    private val autoRefreshPeriod: Duration = 10.seconds
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val taskLists: Flow<List<TaskListUIModel>> = taskRepository.getTaskLists().mapLatest { allLists ->
        allLists.map(TaskListDataModel::asTaskListUIModel)
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private var autoRefreshIsEnabled: Boolean = false

    fun enableAutoRefresh(enabled: Boolean) {
        viewModelScope.launch {
            autoRefreshIsEnabled = enabled
            withContext(Dispatchers.Default) {
                while (autoRefreshIsEnabled) {
                    try {
                        taskRepository.sync()
                    } catch (_: Exception) {
                        // most likely no network
                    }
                    if (autoRefreshIsEnabled) {
                        delay(autoRefreshPeriod)
                    }
                }
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

    fun deleteTaskList(taskListId: TaskListId) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTaskList(taskListId.value)
            } catch (e: Exception) {
                println("Error while deleting task list (${taskListId}): $e")
                // TODO error handling
            }
        }
    }

    fun renameTaskList(taskListId: TaskListId, newTitle: String) {
        viewModelScope.launch {
            try {
                taskRepository.renameTaskList(taskListId.value, newTitle.trim())
            } catch (e: Exception) {
                println("Error creating task: $e")
                // TODO error handling
            }
        }
    }

    fun clearTaskListCompletedTasks(taskListId: TaskListId) {
        viewModelScope.launch {
            try {
                taskRepository.clearTaskListCompletedTasks(taskListId.value)
            } catch (e: Exception) {
                println("Error creating task: $e")
                // TODO error handling
            }
        }
    }

    fun sortBy(taskListId: TaskListId, sorting: TaskListSorting) {
        viewModelScope.launch {
            try {
                taskRepository.sortTasksBy(taskListId.value, sorting)
            } catch (e: Exception) {
                println("Error while sorting task list: $e")
                // TODO error handling
            }
        }
    }

    fun createTask(taskListId: TaskListId, title: String, notes: String = "", dueDate: LocalDate? = null) {
        viewModelScope.launch {
            try {
                taskRepository.createTask(taskListId.value, title, notes, dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault()))
            } catch (e: Exception) {
                println("Error while creating task: $e")
                // TODO error handling
            }
        }
    }

    fun deleteTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId.value)
            } catch (e: Exception) {
                println("Error while deleting task: $e")
                // TODO error handling
            }
        }
    }

    fun confirmTaskDeletion(taskId: TaskId) {
        // TODO(?) purge pending deletion?
    }

    fun restoreTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.restoreTask(taskId.value)
            } catch (e: Exception) {
                println("Error while restoring task: $e")
                // TODO error handling
            }
        }
    }

    fun toggleTaskCompletionState(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletionState(taskId.value)
            } catch (e: Exception) {
                println("Error while toggling task: $e")
                // TODO error handling
            }
        }
    }

    fun updateTask(targetTaskListId: TaskListId, taskId: TaskId, title: String, notes: String, dueDate: LocalDate?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(
                    targetTaskListId.value,
                    taskId.value,
                    title.trim(),
                    notes.trim(),
                    dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
                )
            } catch (e: Exception) {
                println("Error while updating task: $e")
                // TODO error handling
            }
        }
    }

    fun updateTaskTitle(taskId: TaskId, title: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskTitle(taskId.value, title.trim())
            } catch (e: Exception) {
                println("Error while updating task title: $e")
                // TODO error handling
            }
        }
    }

    fun updateTaskNotes(taskId: TaskId, notes: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskNotes(taskId.value, notes.trim())
            } catch (e: Exception) {
                println("Error while updating task notes: $e")
                // TODO error handling
            }
        }
    }

    fun updateTaskDueDate(taskId: TaskId, dueDate: LocalDate?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskDueDate(taskId.value, dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault()))
            } catch (e: Exception) {
                println("Error while updating task due date: $e")
                // TODO error handling
            }
        }
    }

    fun unindentTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.unindentTask(taskId.value)
            } catch (e: Exception) {
                println("Error while indenting task: $e")
                // TODO error handling
            }
        }
    }

    fun indentTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.indentTask(taskId.value)
            } catch (e: Exception) {
                println("Error while indenting task: $e")
                // TODO error handling
            }
        }
    }

    fun moveToTop(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.moveToTop(taskId.value)
            } catch (e: Exception) {
                println("Error while moving task: $e")
                // TODO error handling
            }
        }
    }

    fun moveToList(taskId: TaskId, targetTaskListId: TaskListId) {
        viewModelScope.launch {
            try {
                taskRepository.moveToList(taskId.value, targetTaskListId.value)
            } catch (e: Exception) {
                println("Error while moving task: $e")
                // TODO error handling
            }
        }
    }

    fun moveToNewList(taskId: TaskId, targetTaskListTitle: String) {
        viewModelScope.launch {
            try {
                taskRepository.moveToNewList(taskId.value, targetTaskListTitle.trim())
            } catch (e: Exception) {
                println("Error while moving task: $e")
                // TODO error handling
            }
        }
    }
}
