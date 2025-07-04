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

package net.opatry.tasks.app.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.opatry.logging.Logger
import net.opatry.tasks.DoneTaskPosition
import net.opatry.tasks.TodoTaskPosition
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.presentation.model.compareTo
import net.opatry.tasks.app.ui.TaskEvent
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private inline fun <reified T1, reified T2> List<*>.partitionByType(): Pair<List<T1>, List<T2>> {
    val first = mutableListOf<T1>()
    val second = mutableListOf<T2>()
    for (item in this) {
        when (item) {
            is T1 -> first.add(item)
            is T2 -> second.add(item)
        }
    }
    return first.toList() to second.toList()
}

private fun TaskListDataModel.asTaskListUIModel(isSelected: Boolean): TaskListUIModel {
    val tasksUIModels = tasks.map(TaskDataModel::asTaskUIModel)
    val (completedTasks, remainingTasks) = tasksUIModels.partitionByType<TaskUIModel.Done, TaskUIModel.Todo>()

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
        TaskListSorting.Title -> mapOf(null to remainingTasks)
    }

    return TaskListUIModel(
        id = TaskListId(id),
        title = title,
        remainingTasks = taskGroups.toMap(),
        completedTasks = completedTasks,
        sorting = sorting,
        canDelete = !isDefault,
        isSelected = isSelected,
    )
}

@VisibleForTesting
internal fun TaskDataModel.asTaskUIModel(): TaskUIModel {
    val dueDate = dueDate?.toLocalDateTime(TimeZone.UTC)?.date
    return if (isCompleted) {
        TaskUIModel.Done(
            id = TaskId(id),
            title = title,
            notes = notes,
            dueDate = dueDate,
            completionDate = requireNotNull(completionDate).toLocalDateTime(TimeZone.UTC).date,
            position = DoneTaskPosition.fromPosition(position),
        )
    } else {
        val isFirstTask = position.toIntOrNull() == 0
        TaskUIModel.Todo(
            id = TaskId(id),
            title = title,
            notes = notes,
            dueDate = dueDate,
            position = TodoTaskPosition.fromPosition(position),
            indent = indent,
            canMoveToTop = indent == 0 && !isFirstTask,
            canUnindent = indent > 0,
            canIndent = indent == 0 && !isParentTask && !isFirstTask,
            canCreateSubTask = indent == 0,
        )
    }
}

class TaskListsViewModel(
    private val logger: Logger,
    private val taskRepository: TaskRepository,
    private val autoRefreshPeriod: Duration = 10.seconds
) : ViewModel() {
    private val _selectedTaskListId = MutableStateFlow<TaskListId?>(null)
    val selectedTaskListId = _selectedTaskListId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val taskLists: Flow<List<TaskListUIModel>> = combine(
        selectedTaskListId,
        taskRepository.getTaskLists()
    ) { selectedId, taskLists ->
        taskLists.map { taskList ->
            taskList.asTaskListUIModel(
                isSelected = taskList.id == selectedId?.value
            )
        }
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val _eventFlow = MutableSharedFlow<TaskEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var autoRefreshIsEnabled: Boolean = false

    fun enableAutoRefresh(enabled: Boolean) {
        viewModelScope.launch {
            autoRefreshIsEnabled = enabled
            withContext(Dispatchers.Default) {
                while (autoRefreshIsEnabled) {
                    try {
                        taskRepository.sync()
                    } catch (e: Exception) {
                        // most likely no network
                        logger.logError("Error while syncing", e)
                        // For now, do not notify user, it might flood the UI if network is unreachable
                        // see #114 + #115
                    }
                    if (autoRefreshIsEnabled) {
                        delay(autoRefreshPeriod)
                    }
                }
            }
        }
    }

    fun selectTaskList(taskListId: TaskListId?) {
        viewModelScope.launch {
            _selectedTaskListId.value = taskListId
        }
    }

    fun createTaskList(title: String) {
        viewModelScope.launch {
            try {
                val taskListId = taskRepository.createTaskList(title)
                _selectedTaskListId.value = TaskListId(taskListId)
            } catch (e: Exception) {
                logger.logError("Error while creating task list", e)
                _eventFlow.emit(TaskEvent.Error.TaskList.Create)
            }
        }
    }

    fun deleteTaskList(taskListId: TaskListId) {
        viewModelScope.launch {
            try {
                // TODO select the previous or next one if any
                if (_selectedTaskListId.value == taskListId) {
                    _selectedTaskListId.value = null
                }
                taskRepository.deleteTaskList(taskListId.value)
            } catch (e: Exception) {
                logger.logError("Error while deleting task list (${taskListId})", e)
                _eventFlow.emit(TaskEvent.Error.TaskList.Delete)
            }
        }
    }

    fun repairTaskList(taskListId: TaskListId) {
        // TODO
    }

    fun renameTaskList(taskListId: TaskListId, newTitle: String) {
        viewModelScope.launch {
            try {
                taskRepository.renameTaskList(taskListId.value, newTitle.trim())
            } catch (e: Exception) {
                logger.logError("Error while renaming task list ($taskListId)", e)
                _eventFlow.emit(TaskEvent.Error.TaskList.Rename)
            }
        }
    }

    fun clearTaskListCompletedTasks(taskListId: TaskListId) {
        viewModelScope.launch {
            try {
                taskRepository.clearTaskListCompletedTasks(taskListId.value)
            } catch (e: Exception) {
                logger.logError("Error while clearing completed tasks ($taskListId)", e)
                _eventFlow.emit(TaskEvent.Error.TaskList.ClearCompletedTasks)
            }
        }
    }

    fun sortBy(taskListId: TaskListId, sorting: TaskListSorting) {
        viewModelScope.launch {
            try {
                taskRepository.sortTasksBy(taskListId.value, sorting)
            } catch (e: Exception) {
                logger.logError("Error while sorting task list ($taskListId) by $sorting", e)
                _eventFlow.emit(TaskEvent.Error.TaskList.Sort)
            }
        }
    }

    fun createTask(taskListId: TaskListId, title: String, notes: String = "", dueDate: LocalDate? = null) {
        viewModelScope.launch {
            try {
                _selectedTaskListId.value = taskListId
                taskRepository.createTask(taskListId.value, null, title, notes, dueDate?.atStartOfDayIn(TimeZone.UTC))
            } catch (e: Exception) {
                logger.logError("Error while creating task ($taskListId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Create)
            }
        }
    }

    fun createSubTask(taskListId: TaskListId, parentTaskId: TaskId, title: String, notes: String = "", dueDate: LocalDate? = null) {
        viewModelScope.launch {
            try {
                taskRepository.createTask(
                    taskListId.value,
                    parentTaskId.value,
                    title,
                    notes,
                    dueDate?.atStartOfDayIn(TimeZone.UTC)
                )
            } catch (e: Exception) {
                logger.logError("Error while creating sub task ($taskListId > ${parentTaskId})", e)
                _eventFlow.emit(TaskEvent.Error.Task.CreateChild)
            }
        }
    }

    fun deleteTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId.value)
            } catch (e: Exception) {
                logger.logError("Error while deleting task ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Delete)
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
                logger.logError("Error while restoring task ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Restore)
            }
        }
    }

    fun toggleTaskCompletionState(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletionState(taskId.value)
            } catch (e: Exception) {
                logger.logError("Error while toggling task completion state ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.ToggleCompletionState)
            }
        }
    }

    fun updateTask(taskId: TaskId, title: String, notes: String, dueDate: LocalDate?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(
                    taskId.value,
                    title.trim(),
                    notes.trim(),
                    dueDate?.atStartOfDayIn(TimeZone.UTC)
                )
            } catch (e: Exception) {
                logger.logError("Error while updating task ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Update)
            }
        }
    }

    fun updateTaskTitle(taskId: TaskId, title: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskTitle(taskId.value, title.trim())
            } catch (e: Exception) {
                logger.logError("Error while updating task title ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Update)
            }
        }
    }

    fun updateTaskNotes(taskId: TaskId, notes: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskNotes(taskId.value, notes.trim())
            } catch (e: Exception) {
                logger.logError("Error while updating task notes ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Update)
            }
        }
    }

    fun updateTaskDueDate(taskId: TaskId, dueDate: LocalDate?) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskDueDate(taskId.value, dueDate?.atStartOfDayIn(TimeZone.UTC))
            } catch (e: Exception) {
                logger.logError("Error while updating task due date ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Update)
            }
        }
    }

    fun unindentTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.unindentTask(taskId.value)
            } catch (e: Exception) {
                logger.logError("Error while unindenting task ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Unindent)
            }
        }
    }

    fun indentTask(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.indentTask(taskId.value)
            } catch (e: Exception) {
                logger.logError("Error while indenting task ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Indent)
            }
        }
    }

    fun moveToTop(taskId: TaskId) {
        viewModelScope.launch {
            try {
                taskRepository.moveToTop(taskId.value)
            } catch (e: Exception) {
                logger.logError("Error while moving task to top ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Move)
            }
        }
    }

    fun moveToList(taskId: TaskId, targetTaskListId: TaskListId) {
        viewModelScope.launch {
            try {
                taskRepository.moveToList(taskId.value, targetTaskListId.value)
            } catch (e: Exception) {
                logger.logError("Error while moving task to list ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Move)
            }
        }
    }

    fun moveToNewList(taskId: TaskId, targetTaskListTitle: String) {
        viewModelScope.launch {
            try {
                taskRepository.moveToNewList(taskId.value, targetTaskListTitle.trim())
            } catch (e: Exception) {
                logger.logError("Error while moving task to new list ($taskId)", e)
                _eventFlow.emit(TaskEvent.Error.Task.Move)
            }
        }
    }
}
