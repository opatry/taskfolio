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

package net.opatry.tasks.app.ui.screen

import CalendarX2
import LucideIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.DueDateUpdate.Pick
import net.opatry.tasks.app.ui.component.DueDateUpdate.Reset
import net.opatry.tasks.app.ui.component.DueDateUpdate.Today
import net.opatry.tasks.app.ui.component.DueDateUpdate.Tomorrow
import net.opatry.tasks.app.ui.component.EditTextDialog
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.component.NoTaskListSelectedEmptyState
import net.opatry.tasks.app.ui.component.RowWithIcon
import net.opatry.tasks.app.ui.component.TaskAction
import net.opatry.tasks.app.ui.component.TaskEditMode
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheet
import net.opatry.tasks.app.ui.component.TaskListEditMenuAction
import net.opatry.tasks.app.ui.component.TaskListScaffold
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.task_due_date_reset
import net.opatry.tasks.resources.task_due_date_update_cta
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_confirm
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_message
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_title
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_confirm
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_message
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_title
import net.opatry.tasks.resources.task_list_pane_rename_dialog_cta
import net.opatry.tasks.resources.task_list_pane_rename_dialog_title
import net.opatry.tasks.resources.task_list_pane_task_deleted_snackbar
import net.opatry.tasks.resources.task_list_pane_task_deleted_undo_snackbar
import net.opatry.tasks.resources.task_list_pane_task_restored_snackbar
import net.opatry.tasks.resources.task_menu_move_to_new_list_create_task_list_dialog_confirm
import net.opatry.tasks.resources.task_menu_move_to_new_list_create_task_list_dialog_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetail(viewModel: TaskListsViewModel) {
    val taskLists by viewModel.taskLists.collectAsStateWithLifecycle(null)
    val selectedTaskListId by viewModel.selectedTaskListId.collectAsStateWithLifecycle(null)
    val selectedTaskList = taskLists?.firstOrNull { it.id == selectedTaskListId }

    val lists = taskLists
    val selected = selectedTaskList
    when {
        lists == null -> LoadingPane()
        selected == null -> NoTaskListSelectedEmptyState()
        else -> TaskListDetail(viewModel, lists, selected)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetail(
    viewModel: TaskListsViewModel,
    taskLists: List<TaskListUIModel>,
    selectedTaskList: TaskListUIModel,
) {
    // TODO extract a smart state for all this mess
    var taskOfInterest by remember { mutableStateOf<TaskUIModel?>(null) }

    var showRenameTaskListDialog by remember { mutableStateOf(false) }
    var showClearTaskListCompletedTasksDialog by remember { mutableStateOf(false) }
    var showDeleteTaskListDialog by remember { mutableStateOf(false) }

    var showEditTaskSheet by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showNewTaskSheet by remember { mutableStateOf(false) }
    var showNewSubTaskSheet by remember { mutableStateOf(false) }
    var showNewTaskListAlert by remember { mutableStateOf(false) }

    var showUndoTaskDeletionSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val enableUndoTaskDeletion = false
    if (enableUndoTaskDeletion && showUndoTaskDeletionSnackbar) {
        val taskDeletedMessage = stringResource(Res.string.task_list_pane_task_deleted_snackbar)
        val taskDeletedUndo = stringResource(Res.string.task_list_pane_task_deleted_undo_snackbar)
        val taskRestoredMessage = stringResource(Res.string.task_list_pane_task_restored_snackbar)
        LaunchedEffect(Unit) {
            taskOfInterest?.let { task ->
                val result = snackbarHostState.showSnackbar(
                    message = taskDeletedMessage,
                    actionLabel = taskDeletedUndo,
                    duration = SnackbarDuration.Short
                )
                taskOfInterest = null
                showUndoTaskDeletionSnackbar = false
                when (result) {
                    SnackbarResult.Dismissed -> viewModel.confirmTaskDeletion(task.id)
                    SnackbarResult.ActionPerformed -> {
                        viewModel.restoreTask(task.id)
                        snackbarHostState.showSnackbar(taskRestoredMessage, duration = SnackbarDuration.Short)
                    }
                }
            }
        }
    }

    TaskListScaffold(
        taskLists = taskLists,
        selectedTaskList = selectedTaskList,
        snackbarHostState = snackbarHostState,
        onDeleteList = { showDeleteTaskListDialog = true },
        onRepairList = { viewModel.repairTaskList(selectedTaskList.id) },
        onSortList = { sorting ->
            viewModel.sortBy(selectedTaskList.id, sorting)
        },
        onEditList = { action ->
            when (action) {
                TaskListEditMenuAction.Rename -> showRenameTaskListDialog = true
                TaskListEditMenuAction.ClearCompletedTasks -> showClearTaskListCompletedTasksDialog = true
                TaskListEditMenuAction.Delete -> showDeleteTaskListDialog = true
            }
        },
        onNewTask = { showNewTaskSheet = true },
        onTaskAction = { action ->
            when (action) {
                is TaskAction.ToggleCompletion -> {
                    viewModel.toggleTaskCompletionState(action.task.id)
                }

                is TaskAction.Edit -> {
                    taskOfInterest = action.task
                    showEditTaskSheet = true
                }

                is TaskAction.UpdateDueDate -> {
                    when (action.update) {
                        Pick -> {
                            taskOfInterest = action.task
                            showDatePickerDialog = true
                        }

                        Reset -> viewModel.updateTaskDueDate(action.task.id, null)
                        Today -> viewModel.updateTaskDueDate(action.task.id, Clock.System.todayIn(TimeZone.UTC))
                        Tomorrow -> viewModel.updateTaskDueDate(action.task.id, Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.DAY))
                    }
                }

                is TaskAction.AddSubTask -> {
                    taskOfInterest = action.task
                    showNewSubTaskSheet = true
                }

                is TaskAction.Unindent -> {
                    viewModel.unindentTask(action.task.id)
                }

                is TaskAction.Indent -> {
                    viewModel.indentTask(action.task.id)
                }

                is TaskAction.MoveToTop -> {
                    viewModel.moveToTop(action.task.id)
                }

                is TaskAction.MoveToList -> {
                    viewModel.moveToList(action.task.id, action.targetParentList.id)
                }

                is TaskAction.MoveToNewList -> {
                    taskOfInterest = action.task
                    showNewTaskListAlert = true
                }

                is TaskAction.Delete -> {
                    taskOfInterest = action.task
                    showUndoTaskDeletionSnackbar = true
                    viewModel.deleteTask(action.task.id)
                }
            }
        },
    )

    if (showRenameTaskListDialog) {
        EditTextDialog(
            onDismissRequest = { showRenameTaskListDialog = false },
            onValidate = { newTitle ->
                showRenameTaskListDialog = false
                viewModel.renameTaskList(selectedTaskList.id, newTitle)
            },
            validateLabel = stringResource(Res.string.task_list_pane_rename_dialog_cta),
            dialogTitle = stringResource(Res.string.task_list_pane_rename_dialog_title),
            initialText = selectedTaskList.title,
            allowBlank = false,
        )
    }

    if (showClearTaskListCompletedTasksDialog) {
        AlertDialog(
            onDismissRequest = { showClearTaskListCompletedTasksDialog = false },
            title = {
                Text(stringResource(Res.string.task_list_pane_clear_completed_confirm_dialog_title))
            },
            text = {
                Text(stringResource(Res.string.task_list_pane_clear_completed_confirm_dialog_message))
            },
            dismissButton = {
                TextButton(onClick = { showClearTaskListCompletedTasksDialog = false }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showClearTaskListCompletedTasksDialog = false
                    viewModel.clearTaskListCompletedTasks(selectedTaskList.id)
                }) {
                    Text(stringResource(Res.string.task_list_pane_clear_completed_confirm_dialog_confirm))
                }
            },
        )
    }

    if (showDeleteTaskListDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTaskListDialog = false },
            title = {
                Text(stringResource(Res.string.task_list_pane_delete_list_confirm_dialog_title))
            },
            text = {
                Text(stringResource(Res.string.task_list_pane_delete_list_confirm_dialog_message))
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTaskListDialog = false }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteTaskListDialog = false
                    viewModel.deleteTaskList(selectedTaskList.id)
                }) {
                    Text(stringResource(Res.string.task_list_pane_delete_list_confirm_dialog_confirm))
                }
            },
        )
    }

    // FIXME extract a reusable dialog with proper callbacks instead of putting `if`s everywhere
    if (showEditTaskSheet || showNewTaskSheet || showNewSubTaskSheet) {
        val task = taskOfInterest
        TaskEditorBottomSheet(
            task = task,
            editMode = when {
                showEditTaskSheet -> TaskEditMode.Edit
                showNewSubTaskSheet -> TaskEditMode.NewSubTask
                else -> TaskEditMode.NewTask
            },
            allTaskLists = taskLists,
            selectedTaskList = selectedTaskList,
            // TODO deal with due date and nested alert dialogs
            onEditDueDate = { showDatePickerDialog = true },
            onDismiss = {
                taskOfInterest = null
                showEditTaskSheet = false
                showNewTaskSheet = false
                showNewSubTaskSheet = false
            },
            onValidate = { targetList, title, notes, dueDate ->
                when {
                    showEditTaskSheet -> {
                        taskOfInterest = null
                        showEditTaskSheet = false

                        viewModel.updateTask(requireNotNull(task).id, title, notes, dueDate)
                    }

                    showNewSubTaskSheet -> {
                        taskOfInterest = null
                        showNewSubTaskSheet = false

                        viewModel.createSubTask(targetList.id, requireNotNull(task).id, title, notes, dueDate)
                    }

                    showNewTaskSheet -> {
                        showNewTaskSheet = false

                        viewModel.createTask(targetList.id, title, notes, dueDate)
                    }
                }
            }
        )
    }

    if (showDatePickerDialog) {
        val initialTaskDueDate = taskOfInterest?.dueDate
        taskOfInterest?.let { task ->
            val state = rememberDatePickerState(task.dueDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
            DatePickerDialog(
                onDismissRequest = {
                    taskOfInterest = null
                    showDatePickerDialog = false
                },
                dismissButton = {
                    TextButton(onClick = {
                        taskOfInterest = null
                        showDatePickerDialog = false
                    }) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            taskOfInterest = null
                            showDatePickerDialog = false
                            val newDate = state.selectedDateMillis
                                ?.let(Instant::fromEpochMilliseconds)
                                ?.toLocalDateTime(TimeZone.UTC)
                                ?.date
                            viewModel.updateTaskDueDate(task.id, dueDate = newDate)
                        }
                    ) {
                        Text(stringResource(Res.string.task_due_date_update_cta))
                    }
                },
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePicker(state = state, modifier = Modifier.weight(1f))
                    if (initialTaskDueDate != null) {
                        HorizontalDivider()
                        TextButton(
                            onClick = {
                                showDatePickerDialog = false
                                viewModel.updateTaskDueDate(task.id, null)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            RowWithIcon(
                                text = stringResource(Res.string.task_due_date_reset),
                                icon = LucideIcons.CalendarX2,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewTaskListAlert) {
        val task = checkNotNull(taskOfInterest) { "Can't show new task list dialog without task to move" }
        EditTextDialog(
            onDismissRequest = {
                taskOfInterest = null
                showNewTaskListAlert = false
            },
            validateLabel = stringResource(Res.string.task_menu_move_to_new_list_create_task_list_dialog_confirm),
            onValidate = { title ->
                taskOfInterest = null
                showNewTaskListAlert = false
                viewModel.moveToNewList(task.id, title)
                // TODO should navigate to the newly created list maybe? How?
            },
            dialogTitle = stringResource(Res.string.task_menu_move_to_new_list_create_task_list_dialog_title),
            allowBlank = false
        )
    }
}
