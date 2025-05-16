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

import CalendarDays
import ListPlus
import LucideIcons
import NotepadText
import Plus
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.EditTextDialog
import net.opatry.tasks.app.ui.component.TaskListEditMenuAction
import net.opatry.tasks.app.ui.component.TaskListTopAppBar
import net.opatry.tasks.app.ui.component.TasksColumn
import net.opatry.tasks.app.ui.component.toColor
import net.opatry.tasks.app.ui.component.toLabel
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.task_due_date_update_cta
import net.opatry.tasks.resources.task_editor_sheet_edit_title
import net.opatry.tasks.resources.task_editor_sheet_list_dropdown_label
import net.opatry.tasks.resources.task_editor_sheet_new_subtask_title
import net.opatry.tasks.resources.task_editor_sheet_new_task_title
import net.opatry.tasks.resources.task_editor_sheet_no_due_date_fallback
import net.opatry.tasks.resources.task_editor_sheet_notes_field_label
import net.opatry.tasks.resources.task_editor_sheet_notes_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_title_field_empty_error
import net.opatry.tasks.resources.task_editor_sheet_title_field_label
import net.opatry.tasks.resources.task_editor_sheet_title_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_validate
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
fun TaskListDetail(
    viewModel: TaskListsViewModel,
    taskList: TaskListUIModel,
    onNavigateTo: (TaskListUIModel?) -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsStateWithLifecycle(emptyList())

    // TODO extract a smart state for all this mess
    var taskOfInterest by remember { mutableStateOf<TaskUIModel?>(null) }

    var showRenameTaskListDialog by remember { mutableStateOf(false) }
    var showClearTaskListCompletedTasksDialog by remember { mutableStateOf(false) }
    var showDeleteTaskListDialog by remember { mutableStateOf(false) }

    var showEditTaskSheet by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val taskEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    Scaffold(
        topBar = {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = { sorting ->
                    viewModel.sortBy(taskList.id, sorting)
                },
                onEdit = { action ->
                    when (action) {
                        TaskListEditMenuAction.Rename -> showRenameTaskListDialog = true
                        TaskListEditMenuAction.ClearCompletedTasks -> showClearTaskListCompletedTasksDialog = true
                        TaskListEditMenuAction.Delete -> showDeleteTaskListDialog = true
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        // FIXME should be driven by the NavigationRail
        floatingActionButton = {
            if (!taskList.hasBrokenIndentation()) {
                // FIXME hides bottom of screen
                FloatingActionButton(onClick = { showNewTaskSheet = true }) {
                    Icon(LucideIcons.Plus, null)
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            TasksColumn(
                taskLists,
                taskList,
                onDeleteTaskList = { showDeleteTaskListDialog = true },
                onRepairTaskList = { viewModel.repairTaskList(taskList.id) },
                onToggleCompletionState = { viewModel.toggleTaskCompletionState(it.id) },
                onEditTask = {
                    taskOfInterest = it
                    showEditTaskSheet = true
                },
                onUpdateDueDate = {
                    taskOfInterest = it
                    showDatePickerDialog = true
                },
                onNewSubTask = {
                    taskOfInterest = it
                    showNewSubTaskSheet = true
                },
                onUnindent = { viewModel.unindentTask(it.id) },
                onIndent = { viewModel.indentTask(it.id) },
                onMoveToTop = { viewModel.moveToTop(it.id) },
                onMoveToList = { task, taskList -> viewModel.moveToList(task.id, taskList.id) },
                onMoveToNewList = {
                    taskOfInterest = it
                    showNewTaskListAlert = true
                },
                onDeleteTask = {
                    taskOfInterest = it
                    showUndoTaskDeletionSnackbar = true
                    viewModel.deleteTask(it.id)
                },
            )
        }
    }

    if (showRenameTaskListDialog) {
        EditTextDialog(
            onDismissRequest = { showRenameTaskListDialog = false },
            onValidate = { newTitle ->
                showRenameTaskListDialog = false
                viewModel.renameTaskList(taskList.id, newTitle)
            },
            validateLabel = stringResource(Res.string.task_list_pane_rename_dialog_cta),
            dialogTitle = stringResource(Res.string.task_list_pane_rename_dialog_title),
            initialText = taskList.title,
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
                    viewModel.clearTaskListCompletedTasks(taskList.id)
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
                    viewModel.deleteTaskList(taskList.id)
                    onNavigateTo(null)
                }) {
                    Text(stringResource(Res.string.task_list_pane_delete_list_confirm_dialog_confirm))
                }
            },
        )
    }

    // FIXME extract a reusable dialog with proper callbacks instead of putting `if`s everywhere
    if (showEditTaskSheet || showNewTaskSheet || showNewSubTaskSheet) {
        val task = taskOfInterest
        ModalBottomSheet(
            sheetState = taskEditorSheetState,
            onDismissRequest = {
                taskOfInterest = null
                showEditTaskSheet = false
                showNewTaskSheet = false
                showNewSubTaskSheet = false
            }
        ) {
            val sheetTitleRes = when {
                showEditTaskSheet -> Res.string.task_editor_sheet_edit_title
                showNewSubTaskSheet -> Res.string.task_editor_sheet_new_subtask_title
                else -> Res.string.task_editor_sheet_new_task_title
            }
            // FIXME remembers from 1 dialog to the other
            val initialTitle = task?.title.takeUnless { showNewSubTaskSheet }.orEmpty()
            var newTitle by remember { mutableStateOf(initialTitle) }
            // avoid displaying an error message when user didn't even started to write content
            var alreadyHadSomeContent by remember { mutableStateOf(initialTitle.isNotBlank()) }
            val titleHasError by remember {
                derivedStateOf {
                    newTitle.isBlank()
                }
            }
            // FIXME remembers from 1 dialog to the other
            val initialNotes = task?.notes?.takeUnless { showNewSubTaskSheet }.orEmpty()
            var newNotes by remember { mutableStateOf(initialNotes) }
            var expandTaskListsDropDown by remember { mutableStateOf(false) }
            var targetList by remember { mutableStateOf(taskList) }

            // FIXME doesn't work as expected BottomSheetDefaults.windowInsets.asPaddingValues()
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(sheetTitleRes), style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    newTitle,
                    onValueChange = {
                        alreadyHadSomeContent = alreadyHadSomeContent || it.isNotBlank()
                        newTitle = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.task_editor_sheet_title_field_label)) },
                    placeholder = { Text(stringResource(Res.string.task_editor_sheet_title_field_placeholder)) },
                    maxLines = 1,
                    supportingText = {
                        AnimatedVisibility(visible = titleHasError && alreadyHadSomeContent) {
                            Text(stringResource(Res.string.task_editor_sheet_title_field_empty_error))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    isError = titleHasError && alreadyHadSomeContent,
                )

                OutlinedTextField(
                    newNotes,
                    onValueChange = { newNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.task_editor_sheet_notes_field_label)) },
                    placeholder = { Text(stringResource(Res.string.task_editor_sheet_notes_field_placeholder)) },
                    leadingIcon = { Icon(LucideIcons.NotepadText, null) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )

                // TODO one of top of the other or side by side Date|TaskList
                //  if one on top of the other, need to revise expansion height to display all content
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // TODO Add shortcuts for Today, Tomorrow, Next Week
                    // FIXME when dialog is dismissed, current state is reset but shouldn't need to extract date picker dialog use
                    val dueDateLabel = task?.dateRange?.toLabel()?.takeUnless(String::isBlank)
                        ?: stringResource(Res.string.task_editor_sheet_no_due_date_fallback)
                    AssistChip(
                        onClick = { showDatePickerDialog = true },
                        enabled = false, // TODO not supported for now, super imposed dialogs breaks the flow
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(LucideIcons.CalendarDays, null) },
                        label = { Text(dueDateLabel, color = task?.dateRange.toColor()) },
                    )

                    val enableAdvancedTaskEdit = false // TODO implement advanced task edit
                    if (enableAdvancedTaskEdit && showEditTaskSheet) {
                        ExposedDropdownMenuBox(
                            expanded = expandTaskListsDropDown,
                            onExpandedChange = { expandTaskListsDropDown = it }
                        ) {
                            OutlinedTextField(
                                targetList.title,
                                onValueChange = {},
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                                label = { Text(stringResource(Res.string.task_editor_sheet_list_dropdown_label)) },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandTaskListsDropDown)
                                },
                            )

                            ExposedDropdownMenu(
                                expanded = expandTaskListsDropDown,
                                onDismissRequest = { expandTaskListsDropDown = false }
                            ) {
                                taskLists.forEach { taskList ->
                                    DropdownMenuItem(
                                        text = { Text(taskList.title) },
                                        onClick = {
                                            targetList = taskList
                                            expandTaskListsDropDown = false
                                        }
                                    )
                                }
                            }
                        }

                        // TODO instead of a button opening a dialog, TextField could be editable to let user
                        //  enter the new list name, if targetList isn't part of taskLists, then, it's a new one
                        IconButton(onClick = {}) {
                            Icon(LucideIcons.ListPlus, null)
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        taskOfInterest = null
                        showEditTaskSheet = false
                        showNewTaskSheet = false
                        showNewSubTaskSheet = false
                    }) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                    Button(
                        onClick = {
                            when {
                                showEditTaskSheet -> {
                                    taskOfInterest = null
                                    showEditTaskSheet = false

                                    // TODO deal with due date and nested alert dialogs
                                    viewModel.updateTask(requireNotNull(task).id, newTitle, newNotes, task.dueDate /*FIXME*/)
                                }

                                showNewSubTaskSheet -> {
                                    taskOfInterest = null
                                    showNewSubTaskSheet = false

                                    // TODO deal with due date and nested alert dialogs
                                    viewModel.createSubTask(targetList.id, requireNotNull(task).id, newTitle, newNotes, task.dueDate /*FIXME*/)
                                }

                                showNewTaskSheet -> {
                                    showNewTaskSheet = false

                                    onNavigateTo(targetList)
                                    viewModel.createTask(targetList.id, newTitle, newNotes, null /*TODO*/)
                                }
                            }
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text(stringResource(Res.string.task_editor_sheet_validate))
                    }
                }
            }
        }
    }

    if (showDatePickerDialog) {
        taskOfInterest?.let { task ->
            val state = rememberDatePickerState(task.dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds())
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
                    Button(onClick = {
                        taskOfInterest = null
                        showDatePickerDialog = false
                        val newDate = state.selectedDateMillis
                            ?.let(Instant::fromEpochMilliseconds)
                            ?.toLocalDateTime(TimeZone.currentSystemDefault())
                            ?.date
                        viewModel.updateTaskDueDate(task.id, dueDate = newDate)
                    }) {
                        Text(stringResource(Res.string.task_due_date_update_cta))
                    }
                },
            ) {
                DatePicker(state)
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
