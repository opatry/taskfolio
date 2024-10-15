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

package net.opatry.tasks.app.ui.screen

import CalendarDays
import CheckCheck
import ChevronDown
import ChevronRight
import Circle
import CircleCheckBig
import CircleOff
import EllipsisVertical
import LayoutList
import ListPlus
import LucideIcons
import NotepadText
import Plus
import Trash
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.component.EditTextDialog
import net.opatry.tasks.app.ui.component.EmptyState
import net.opatry.tasks.app.ui.component.MissingScreen
import net.opatry.tasks.app.ui.component.RowWithIcon
import net.opatry.tasks.app.ui.component.TaskListMenu
import net.opatry.tasks.app.ui.component.TaskListMenuAction
import net.opatry.tasks.app.ui.component.TaskMenu
import net.opatry.tasks.app.ui.component.TaskMenuAction
import net.opatry.tasks.app.ui.model.DateRange
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.app.ui.model.compareTo
import net.opatry.tasks.app.ui.tooling.TaskfolioPreview
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.task_due_date_label_days_ago
import net.opatry.tasks.resources.task_due_date_label_no_date
import net.opatry.tasks.resources.task_due_date_label_past
import net.opatry.tasks.resources.task_due_date_label_today
import net.opatry.tasks.resources.task_due_date_label_tomorrow
import net.opatry.tasks.resources.task_due_date_label_weeks_ago
import net.opatry.tasks.resources.task_due_date_label_yesterday
import net.opatry.tasks.resources.task_due_date_update_cta
import net.opatry.tasks.resources.task_editor_sheet_edit_title
import net.opatry.tasks.resources.task_editor_sheet_list_dropdown_label
import net.opatry.tasks.resources.task_editor_sheet_new_title
import net.opatry.tasks.resources.task_editor_sheet_no_due_date_fallback
import net.opatry.tasks.resources.task_editor_sheet_notes_field_label
import net.opatry.tasks.resources.task_editor_sheet_notes_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_title_field_empty_error
import net.opatry.tasks.resources.task_editor_sheet_title_field_label
import net.opatry.tasks.resources.task_editor_sheet_title_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_validate
import net.opatry.tasks.resources.task_list_pane_all_tasks_complete_desc
import net.opatry.tasks.resources.task_list_pane_all_tasks_complete_title
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_confirm
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_message
import net.opatry.tasks.resources.task_list_pane_clear_completed_confirm_dialog_title
import net.opatry.tasks.resources.task_list_pane_completed_section_title_with_count
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_confirm
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_message
import net.opatry.tasks.resources.task_list_pane_delete_list_confirm_dialog_title
import net.opatry.tasks.resources.task_list_pane_delete_task_icon_content_desc
import net.opatry.tasks.resources.task_list_pane_rename_dialog_cta
import net.opatry.tasks.resources.task_list_pane_rename_dialog_title
import net.opatry.tasks.resources.task_list_pane_task_deleted_snackbar
import net.opatry.tasks.resources.task_list_pane_task_deleted_undo_snackbar
import net.opatry.tasks.resources.task_list_pane_task_options_icon_content_desc
import net.opatry.tasks.resources.task_list_pane_task_restored_snackbar
import net.opatry.tasks.resources.task_lists_screen_empty_list_desc
import net.opatry.tasks.resources.task_lists_screen_empty_list_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

enum class TaskListSorting {
    Manual,
    DueDate,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetail(
    viewModel: TaskListsViewModel,
    taskList: TaskListUIModel,
    onNavigateTo: (TaskListUIModel?) -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsState(emptyList())
    var taskSorting by remember { mutableStateOf(TaskListSorting.Manual) }

    // TODO extract a smart state for all this mess
    var taskOfInterest by remember { mutableStateOf<TaskUIModel?>(null) }

    var showTaskListActions by remember { mutableStateOf(false) }
    var showRenameTaskListDialog by remember { mutableStateOf(false) }
    var showClearTaskListCompletedTasksDialog by remember { mutableStateOf(false) }
    var showDeleteTaskListDialog by remember { mutableStateOf(false) }

    var showEditTaskSheet by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val taskEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNewTaskSheet by remember { mutableStateOf(false) }
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
                    SnackbarResult.Dismissed -> viewModel.confirmTaskDeletion(task)
                    SnackbarResult.ActionPerformed -> {
                        viewModel.restoreTask(task)
                        snackbarHostState.showSnackbar(taskRestoredMessage, duration = SnackbarDuration.Short)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            // FIXME tweak colors, elevation, etc.
            TopAppBar(
                title = {
                    Text(text = taskList.title, style = MaterialTheme.typography.headlineSmall)
                },
                actions = {
                    IconButton(onClick = { showTaskListActions = true }) {
                        Icon(LucideIcons.EllipsisVertical, null)
                    }
                    TaskListMenu(taskList, showTaskListActions, taskSorting) { action ->
                        showTaskListActions = false
                        when (action) {
                            TaskListMenuAction.Dismiss -> Unit
                            TaskListMenuAction.SortManual -> taskSorting = TaskListSorting.Manual
                            TaskListMenuAction.SortDate -> taskSorting = TaskListSorting.DueDate
                            TaskListMenuAction.Rename -> showRenameTaskListDialog = true
                            TaskListMenuAction.ClearCompletedTasks -> showClearTaskListCompletedTasksDialog = true
                            TaskListMenuAction.Delete -> showDeleteTaskListDialog = true
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        // FIXME should be driven by the NavigationRail
        floatingActionButton = {
            // FIXME hides bottom of screen
            FloatingActionButton(onClick = { showNewTaskSheet = true }) {
                Icon(LucideIcons.Plus, null)
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            if (taskList.isEmpty) {
                // TODO SVG undraw.co illustration `files/undraw_to_do_list_re_9nt7.svg`
                EmptyState(
                    icon = LucideIcons.CircleOff,
                    title = stringResource(Res.string.task_lists_screen_empty_list_title),
                    description = stringResource(Res.string.task_lists_screen_empty_list_desc),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                TasksColumn(
                    taskLists,
                    taskList,
                    taskSorting,
                    onToggleCompletionState = viewModel::toggleTaskCompletionState,
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
                        showNewTaskSheet = true
                    },
                    onUnindent = viewModel::unindentTask,
                    onIndent = viewModel::indentTask,
                    onMoveToTop = viewModel::moveToTop,
                    onMoveToList = viewModel::moveToList,
                    onMoveToNewList = {
                        taskOfInterest = it
                        showNewTaskListAlert = true
                    },
                    onDeleteTask = {
                        taskOfInterest = it
                        showUndoTaskDeletionSnackbar = true
                        viewModel.deleteTask(it)
                    },
                )
            }
        }
    }

    if (showRenameTaskListDialog) {
        EditTextDialog(
            onDismissRequest = { showRenameTaskListDialog = false },
            onValidate = { newTitle ->
                showRenameTaskListDialog = false
                viewModel.renameTaskList(taskList, newTitle)
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
                    viewModel.clearTaskListCompletedTasks(taskList)
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
                    viewModel.deleteTaskList(taskList)
                    onNavigateTo(null)
                }) {
                    Text(stringResource(Res.string.task_list_pane_delete_list_confirm_dialog_confirm))
                }
            },
        )
    }

    // FIXME extract a reusable dialog with proper callbacks instead of putting `if`s everywhere
    if (showEditTaskSheet || showNewTaskSheet) {
        val task = taskOfInterest
        ModalBottomSheet(
            sheetState = taskEditorSheetState,
            onDismissRequest = {
                taskOfInterest = null
                showEditTaskSheet = false
                showNewTaskSheet = false
            }
        ) {
            val sheetTitleRes = if (showEditTaskSheet) Res.string.task_editor_sheet_edit_title else Res.string.task_editor_sheet_new_title
            var newTitle by remember { mutableStateOf(task?.title ?: "") }
            val titleHasError by remember {
                derivedStateOf {
                    showEditTaskSheet && newTitle.isBlank()
                }
            }
            var newNotes by remember { mutableStateOf(task?.notes ?: "") }
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
                    onValueChange = { newTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.task_editor_sheet_title_field_label)) },
                    placeholder = { Text(stringResource(Res.string.task_editor_sheet_title_field_placeholder)) },
                    maxLines = 1,
                    supportingText = {
                        AnimatedVisibility(visible = titleHasError) {
                            Text(stringResource(Res.string.task_editor_sheet_title_field_empty_error))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    isError = titleHasError,
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

                    val enableAdvancedTaskEdit = false
                    if (enableAdvancedTaskEdit) {
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
                    }) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                    Button(
                        onClick = {
                            if (showEditTaskSheet) {
                                taskOfInterest = null
                                showEditTaskSheet = false

                                // TODO deal with due date and nested alert dialogs
                                viewModel.updateTask(targetList, requireNotNull(task), newTitle, newNotes, task.dueDate /*FIXME*/)
                            } else if (showNewTaskSheet) {
                                showNewTaskSheet = false

                                onNavigateTo(targetList)
                                viewModel.createTask(targetList, newTitle, newNotes, null /*TODO*/)
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
                        viewModel.updateTaskDueDate(task, dueDate = newDate)
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
        // FIXME should be a dialog
        ModalBottomSheet(onDismissRequest = {
            taskOfInterest = null
            showNewTaskListAlert = false
        }) {
            MissingScreen("New task list", LucideIcons.LayoutList)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksColumn(
    taskLists: List<TaskListUIModel>,
    taskList: TaskListUIModel,
    taskSorting: TaskListSorting,
    onToggleCompletionState: (TaskUIModel) -> Unit,
    onEditTask: (TaskUIModel) -> Unit,
    onUpdateDueDate: (TaskUIModel) -> Unit,
    onNewSubTask: (TaskUIModel) -> Unit,
    onUnindent: (TaskUIModel) -> Unit,
    onIndent: (TaskUIModel) -> Unit,
    onMoveToTop: (TaskUIModel) -> Unit,
    onMoveToList: (TaskUIModel, TaskListUIModel) -> Unit,
    onMoveToNewList: (TaskUIModel) -> Unit,
    onDeleteTask: (TaskUIModel) -> Unit,
) {
    var showCompleted by remember(taskList.id) { mutableStateOf(false) }

    // FIXME remember computation & derived states
    val (completedTasks, remainingTasks) = taskList.tasks.partition(TaskUIModel::isCompleted)

    val taskGroups = when (taskSorting) {
        // no grouping
        TaskListSorting.Manual -> mapOf(null to remainingTasks)
        TaskListSorting.DueDate -> remainingTasks
            .map { it.copy(indent = 0) }
            .sortedWith { o1, o2 -> o1.dateRange.compareTo(o2.dateRange) }
            .groupBy { task ->
                when (task.dateRange) {
                    // merge all overdue tasks to the same range
                    is DateRange.Overdue -> DateRange.Overdue(LocalDate.fromEpochDays(-1), 1)

                    else -> task.dateRange
                }
            }
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (completedTasks.isNotEmpty() && remainingTasks.isEmpty()) {
            item(key = "all_tasks_complete") {
                EmptyState(
                    icon = LucideIcons.CheckCheck,
                    title = stringResource(Res.string.task_list_pane_all_tasks_complete_title),
                    description = stringResource(Res.string.task_list_pane_all_tasks_complete_desc),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        }

        taskGroups.forEach { (dateRange, tasks) ->
            if (dateRange != null) {
                stickyHeader(key = dateRange) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            dateRange.toLabel(sectionLabel = true),
                            style = MaterialTheme.typography.titleSmall,
                            color = dateRange.toColor(),
                        )
                    }
                }
            }
            items(tasks, key = TaskUIModel::id) { task ->
                TaskRow(
                    taskLists,
                    task,
                    showDate = taskSorting == TaskListSorting.Manual,
                    onToggleCompletionState = { onToggleCompletionState(task) },
                    onEditTask = { onEditTask(task) },
                    onUpdateDueDate = { onUpdateDueDate(task) },
                    onNewSubTask = { onNewSubTask(task) },
                    onUnindent = { onUnindent(task) },
                    onIndent = { onIndent(task) },
                    onMoveToTop = { onMoveToTop(task) },
                    onMoveToList = { onMoveToList(task, it) },
                    onMoveToNewList = { onMoveToNewList(task) },
                    onDeleteTask = { onDeleteTask(task) },
                )
            }
        }

        if (completedTasks.isNotEmpty()) {
            stickyHeader(key = "completed") {
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.large)
                        .fillMaxWidth()
                        .clickable { showCompleted = !showCompleted }
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    RowWithIcon(
                        icon = {
                            if (showCompleted) {
                                Icon(LucideIcons.ChevronDown, null)
                            } else {
                                Icon(LucideIcons.ChevronRight, null)
                            }
                        }
                    ) {
                        Text(
                            stringResource(Res.string.task_list_pane_completed_section_title_with_count, completedTasks.size),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
        if (showCompleted) {
            items(completedTasks, key = TaskUIModel::id) { task ->
                TaskRow(
                    taskLists,
                    task,
                    showDate = true,
                    onToggleCompletionState = { onToggleCompletionState(task) },
                    onEditTask = { onEditTask(task) },
                    onUpdateDueDate = { onUpdateDueDate(task) },
                    onNewSubTask = { onNewSubTask(task) },
                    onUnindent = { onUnindent(task) },
                    onIndent = { onIndent(task) },
                    onMoveToTop = { onMoveToTop(task) },
                    onMoveToList = { onMoveToList(task, it) },
                    onMoveToNewList = { onMoveToNewList(task) },
                    onDeleteTask = { onDeleteTask(task) },
                )
            }
        }
    }
}

@Composable
fun DateRange?.toColor(): Color = when (this) {
    is DateRange.Overdue -> MaterialTheme.colorScheme.error

    is DateRange.Today -> MaterialTheme.colorScheme.primary
    is DateRange.Later,
    DateRange.None,
    null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
}

@Composable
fun DateRange.toLabel(sectionLabel: Boolean = false): String = when (this) {
    is DateRange.Overdue -> {
        val numberOfDays = abs(numberOfDays)
        when {
            sectionLabel -> stringResource(Res.string.task_due_date_label_past)
            numberOfDays == 1 -> stringResource(Res.string.task_due_date_label_yesterday)
            numberOfDays < 7 -> stringResource(Res.string.task_due_date_label_days_ago, numberOfDays)
            else -> stringResource(Res.string.task_due_date_label_weeks_ago, numberOfDays / 7)
        }
    }

    is DateRange.Today -> stringResource(Res.string.task_due_date_label_today)
    is DateRange.Later -> when {
        numberOfDays == 1 -> stringResource(Res.string.task_due_date_label_tomorrow)
        // TODO localize names & format
        date.year == Clock.System.todayIn(TimeZone.currentSystemDefault()).year -> LocalDate.Format {
            // FIXME doesn't work with more than 2 dd or MM
            //  byUnicodePattern("ddd', 'MMM' 'yyyy")
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // TODO translation
            chars(", ")
            monthName(MonthNames.ENGLISH_ABBREVIATED) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
        }.format(date)

        else -> LocalDate.Format {
            // FIXME doesn't work with more than 2 MM
            //  byUnicodePattern("MMMM' 'dd', 'yyyy")
            monthName(MonthNames.ENGLISH_FULL) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
            chars(", ")
            year()
        }.format(date)
    }

    DateRange.None -> when {
        sectionLabel -> stringResource(Res.string.task_due_date_label_no_date)
        else -> ""
    }
}

@Composable
fun TaskRow(
    taskLists: List<TaskListUIModel>,
    task: TaskUIModel,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onToggleCompletionState: () -> Unit = {},
    onEditTask: () -> Unit = {},
    onUpdateDueDate: () -> Unit = {},
    onNewSubTask: () -> Unit = {},
    onUnindent: () -> Unit = {},
    onIndent: () -> Unit = {},
    onMoveToTop: () -> Unit = {},
    onMoveToList: (TaskListUIModel) -> Unit = {},
    onMoveToNewList: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
) {
    var showContextualMenu by remember { mutableStateOf(false) }

    // TODO remember?
    val (taskCheckIcon, taskCheckIconColor) = when {
        task.isCompleted -> LucideIcons.CircleCheckBig to MaterialTheme.colorScheme.primary
        else -> LucideIcons.Circle to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Row(modifier.clickable(onClick = onEditTask)) {
        IconButton(
            onClick = onToggleCompletionState,
            Modifier.padding(start = 36.dp * task.indent)
        ) {
            Icon(taskCheckIcon, null, tint = taskCheckIconColor)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                task.title,
                textDecoration = TextDecoration.LineThrough.takeIf { task.isCompleted },
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            if (task.notes.isNotBlank()) {
                Text(
                    task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            if (showDate && task.dueDate != null) {
                AssistChip(
                    onClick = onUpdateDueDate,
                    shape = MaterialTheme.shapes.large,
                    label = {
                        Text(
                            task.dateRange.toLabel(),
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else
                                task.dateRange.toColor()
                        )
                    },
                )
            }
        }
        if (task.isCompleted) {
            IconButton(onClick = onDeleteTask) {
                Icon(LucideIcons.Trash, stringResource(Res.string.task_list_pane_delete_task_icon_content_desc))
            }
        } else {
            Box {
                IconButton(onClick = { showContextualMenu = true }) {
                    Icon(LucideIcons.EllipsisVertical, stringResource(Res.string.task_list_pane_task_options_icon_content_desc))
                }
                TaskMenu(taskLists, task, showContextualMenu) { action ->
                    showContextualMenu = false
                    when (action) {
                        TaskMenuAction.Dismiss -> Unit
                        TaskMenuAction.AddSubTask -> onNewSubTask()
                        TaskMenuAction.Indent -> onIndent()
                        TaskMenuAction.Unindent -> onUnindent()
                        TaskMenuAction.MoveToTop -> onMoveToTop()
                        is TaskMenuAction.MoveToList -> onMoveToList(action.targetParentList)
                        TaskMenuAction.MoveToNewList -> onMoveToNewList()
                        TaskMenuAction.Delete -> onDeleteTask()
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRowScaffold(
    title: String = "My task",
    notes: String = "",
    dueDate: LocalDate? = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    isCompleted: Boolean = false
) {
    TaskRow(
        emptyList(),
        TaskUIModel(
            id = 0L,
            title = title,
            notes = notes,
            dueDate = dueDate,
            isCompleted = isCompleted
        ),
    )
}

@TaskfolioPreview
@Composable
private fun TaskRowDatesPreview() {
    TaskfolioThemedPreview {
        Column {
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(180, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(3, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(1, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(10, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(500, DateTimeUnit.DAY))
        }
    }
}

@TaskfolioPreview
@Composable
private fun TaskRowValuesPreview() {
    TaskfolioThemedPreview {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskRowScaffold(dueDate = null)
            TaskRowScaffold(notes = "This is some details")
            TaskRowScaffold(notes = "This is some details about the task\nsdfsdfsd sdfsdf sdf sdfsd f\ns fsdfsd fsdfdsf f\n", dueDate = null)
            TaskRowScaffold(title = "My completed task", notes = "This is some details", isCompleted = true)
        }
    }
}
