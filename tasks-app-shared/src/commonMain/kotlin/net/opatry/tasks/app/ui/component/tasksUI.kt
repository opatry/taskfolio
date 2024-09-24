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

package net.opatry.tasks.app.ui.component

import ChevronDown
import ChevronRight
import Circle
import CircleCheckBig
import CircleFadingPlus
import CircleOff
import EllipsisVertical
import LayoutList
import LucideIcons
import Pen
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.model.DateRange
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TasksAppPreview
import net.opatry.tasks.app.ui.tooling.TasksAppThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_empty_list_desc
import net.opatry.tasks.resources.task_lists_screen_empty_list_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDetail(
    viewModel: TaskListsViewModel,
    taskList: TaskListUIModel,
    onBack: () -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsState(emptyList())

    var showTaskListActions by remember { mutableStateOf(false) }
    var showRenameTaskListDialog by remember { mutableStateOf(false) }
    var showClearTaskListCompletedTasksDialog by remember { mutableStateOf(false) }
    var showDeleteTaskListDialog by remember { mutableStateOf(false) }

    var showEditTaskSheet by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val editTaskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showNewTaskSheet by remember { mutableStateOf(false) }
    val newTaskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showNewTaskListAlert by remember { mutableStateOf(false) }

    var showUndoTaskDeletionSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showUndoTaskDeletionSnackbar) {
        LaunchedEffect(Unit) {
            val result = snackbarHostState.showSnackbar(
                message = "Task deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed -> {
                    // TODO?
                }
                SnackbarResult.ActionPerformed -> {
                    // TODO
                    snackbarHostState.showSnackbar("Task restored")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            // FIXME tweak colors, elevation, etc.
            TopAppBar(
                title = {
                    // FIXME title not updated on rename action
                    Text(text = taskList.title, style = MaterialTheme.typography.headlineSmall)
                },
                actions = {
                    IconButton(onClick = { showTaskListActions = true }) {
                        Icon(LucideIcons.EllipsisVertical, null)
                    }
                    TaskListMenu(taskList, showTaskListActions) { action ->
                        showTaskListActions = false
                        when (action) {
                            TaskListMenuAction.Dismiss -> Unit
                            TaskListMenuAction.SortManual -> {}
                            TaskListMenuAction.SortDate -> {}
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
    ) {
        // FIXME tasks not updated on delete action
        Box(Modifier.padding(it)) {
            if (taskList.isEmpty) {
                // TODO SVG undraw.co illustration `files/undraw_to_do_list_re_9nt7.svg`
                EmptyState(
                    icon = LucideIcons.CircleOff,
                    title = stringResource(Res.string.task_lists_screen_empty_list_title),
                    description = stringResource(Res.string.task_lists_screen_empty_list_desc),
                )
            } else {
                TasksColumn(
                    taskLists,
                    taskList.tasks,
                    onToggleCompletionState = viewModel::toggleTaskCompletionState,
                    onEditTask = { showEditTaskSheet = true },
                    onUpdateDueDate = { showDatePickerDialog = true },
                    onNewSubTask = { showNewTaskSheet = true },
                    onUnindent = {},
                    onIndent = {},
                    onMoveToTop = {},
                    onMoveInList = { task, newParentList -> },
                    onMoveInNewList = { showNewTaskListAlert = true },
                    onDeleteTask = { showUndoTaskDeletionSnackbar = true },
                )
            }
        }
    }

    if (showRenameTaskListDialog) {
        Dialog(
            onDismissRequest = { showRenameTaskListDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            var newTitle by remember { mutableStateOf(taskList.title) }
            val hasError by remember {
                derivedStateOf {
                    newTitle.isBlank()
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Rename list", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        newTitle,
                        onValueChange = { newTitle = it },
                        maxLines = 1,
                        supportingText = {
                            AnimatedVisibility(visible = hasError) {
                                Text("Title cannot be empty")
                            }
                        },
                        isError = hasError,
                    )
                    Row(
                        Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { showRenameTaskListDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showRenameTaskListDialog = false
                                viewModel.renameTaskList(taskList, newTitle)
                            },
                            enabled = !hasError
                        ) {
                            Text("Rename")
                        }
                    }
                }
            }
        }
    }

    if (showClearTaskListCompletedTasksDialog) {
        AlertDialog(
            onDismissRequest = { showClearTaskListCompletedTasksDialog = false },
            title = {
                Text("Clear all completed tasks?")
            },
            text = {
                Text("All completed tasks will be permanently deleted from this list.")
            },
            dismissButton = {
                TextButton(onClick = { showClearTaskListCompletedTasksDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showClearTaskListCompletedTasksDialog = false
                    viewModel.clearTaskListCompletedTasks(taskList)
                }) {
                    Text("Clear")
                }
            },
        )
    }

    if (showDeleteTaskListDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTaskListDialog = false },
            title = {
                Text("Delete this list?")
            },
            text = {
                Text("All tasks in this list will be permanently deleted.")
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTaskListDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteTaskListDialog = false
                    viewModel.deleteTaskList(taskList)
                    onBack()
                }) {
                    Text("Delete")
                }
            },
        )
    }

    if (showEditTaskSheet) {
        ModalBottomSheet(
            sheetState = editTaskSheetState,
            onDismissRequest = { showEditTaskSheet = false }
        ) {
            MissingScreen("Edit task", LucideIcons.Pen)
        }
    }

    if (showDatePickerDialog) {
        // TODO task.dueDate
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDatePickerDialog = false
                    val newDate = state.selectedDateMillis?.let(Instant::fromEpochMilliseconds)?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    println(newDate)
                    // TODO
                }) {
                    Text("Update")
                }
            },
        ) {
            DatePicker(state)
        }
    }

    if (showNewTaskSheet) {
        ModalBottomSheet(
            sheetState = newTaskSheetState,
            onDismissRequest = { showNewTaskSheet = false }
        ) {
                MissingScreen("New task", LucideIcons.CircleFadingPlus)
        }
    }

    if (showNewTaskListAlert) {
        AlertDialog(onDismissRequest = { showNewTaskListAlert = false }) {
            MissingScreen("New task list", LucideIcons.LayoutList)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksColumn(
    taskLists: List<TaskListUIModel>,
    tasks: List<TaskUIModel>,
    onToggleCompletionState: (TaskUIModel) -> Unit,
    onEditTask: (TaskUIModel) -> Unit,
    onUpdateDueDate: (TaskUIModel) -> Unit,
    onNewSubTask: (TaskUIModel) -> Unit,
    onUnindent: (TaskUIModel) -> Unit,
    onIndent: (TaskUIModel) -> Unit,
    onMoveToTop: (TaskUIModel) -> Unit,
    onMoveInList: (TaskUIModel, TaskListUIModel) -> Unit,
    onMoveInNewList: (TaskUIModel) -> Unit,
    onDeleteTask: (TaskUIModel) -> Unit,
) {
    var showCompleted by remember(tasks) { mutableStateOf(false) }

    // TODO depending on sorting (manual vs date), sections could be different
    //  manual: no section title for not completed tasks, expandable "completed" section
    //  date: sections by date cluster (past, today, tomorrow, future), expandable "completed" section

    // FIXME remember computation & derived states
    val groupedTasks = tasks.sortedBy { it.isCompleted }.groupBy { it.isCompleted }.toMutableMap()
    val completedCount = groupedTasks[true]?.size ?: 0
    if (!showCompleted) {
        groupedTasks[true] = emptyList()
    }

    Column {
        TextButton(onClick = {}) {
            RowWithIcon("Add task", LucideIcons.CircleFadingPlus)
        }

        HorizontalDivider()

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedTasks.forEach { (completed, tasks) ->
                if (completed && completedCount > 0) {
                    stickyHeader {
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
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
                                    "Completed (${completedCount})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
                items(tasks) { task ->
                    TaskRow(
                        taskLists,
                        task,
                        onToggleCompletionState = { onToggleCompletionState(task) },
                        onEditTask = { onEditTask(task) },
                        onUpdateDueDate = { onUpdateDueDate(task) },
                        onNewSubTask = { onNewSubTask(task) },
                        onUnindent = { onUnindent(task) },
                        onIndent = { onIndent(task) },
                        onMoveToTop = { onMoveToTop(task) },
                        onMoveInList = { onMoveInList(task, it) },
                        onMoveInNewList = { onMoveInNewList(task) },
                        onDeleteTask = { onDeleteTask(task) },
                    )
                }
            }
        }
    }
}

@Composable
fun DateRange.toColor(): Color = when (this) {
    is DateRange.Overdue,
    DateRange.Yesterday -> MaterialTheme.colorScheme.error

    DateRange.Today -> MaterialTheme.colorScheme.primary
    DateRange.Tomorrow,
    is DateRange.Later,
    DateRange.None -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
}

@Composable
fun DateRange.toLabel(): String = when (this) {
    is DateRange.Overdue -> {
        // TODO string resources with quantity
        if (numberOfDays < 7) {
            "$numberOfDays days ago"
        } else {
            "${numberOfDays / 7} weeks ago"
        }
    }

    DateRange.Yesterday -> "Yesterday"
    DateRange.Today -> "Today"
    DateRange.Tomorrow -> "Tomorrow"
    // TODO nicer formatting with "Jvm" formatter (localized)
    is DateRange.Later -> if (date.year != Clock.System.todayIn(TimeZone.currentSystemDefault()).year) {
        "${date.month.name} ${date.dayOfMonth}, ${date.year}"
    } else {
        "${date.dayOfWeek.name}, ${date.dayOfMonth}, ${date.year}"
    }

    DateRange.None -> ""
}

@Composable
fun TaskRow(
    taskLists: List<TaskListUIModel>,
    task: TaskUIModel,
    onToggleCompletionState: () -> Unit,
    onEditTask: () -> Unit,
    onUpdateDueDate: () -> Unit,
    onNewSubTask: () -> Unit,
    onUnindent: () -> Unit,
    onIndent: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveInList: (TaskListUIModel) -> Unit,
    onMoveInNewList: () -> Unit,
    onDeleteTask: () -> Unit,
) {
    var showContextualMenu by remember { mutableStateOf(false) }

    // TODO remember?
    val (taskCheckIcon, taskCheckIconColor) = when {
        task.isCompleted -> LucideIcons.CircleCheckBig to MaterialTheme.colorScheme.primary
        else -> LucideIcons.Circle to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Row(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEditTask)
    ) {
        IconButton(
            onClick = onToggleCompletionState,
            Modifier.padding(start = 24.dp * task.indent)
        ) {
            Icon(taskCheckIcon, null, tint = taskCheckIconColor)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)) {
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
            if (task.dueDate != null) {
                AssistChip(
                    onClick = onUpdateDueDate,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(task.dateRange.toLabel(), color = task.dateRange.toColor()) },
                )
            }
        }
        if (task.isCompleted) {
            IconButton(onClick = onDeleteTask) {
                Icon(LucideIcons.Trash, "Delete task")
            }
        } else {
            Box {
                IconButton(onClick = { showContextualMenu = true }) {
                    Icon(LucideIcons.EllipsisVertical, "Task options")
                }
                TaskMenu(taskLists, task, showContextualMenu) { action ->
                    showContextualMenu = false
                    when (action) {
                        TaskMenuAction.Dismiss -> Unit
                        TaskMenuAction.AddSubTask -> onNewSubTask()
                        TaskMenuAction.Indent -> onIndent()
                        TaskMenuAction.Unindent -> onUnindent()
                        TaskMenuAction.MoveToTop -> onMoveToTop()
                        is TaskMenuAction.MoveInList -> onMoveInList(action.newParentList)
                        TaskMenuAction.MoveInNewList -> onMoveInNewList()
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
        {},
        {},
        {},
        {},
        {},
        {},
        {},
        {},
        {},
        {}
    )
}

@TasksAppPreview
@Composable
private fun TaskRowDatesPreview() {
    TasksAppThemedPreview {
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

@TasksAppPreview
@Composable
private fun TaskRowValuesPreview() {
    TasksAppThemedPreview {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskRowScaffold(dueDate = null)
            TaskRowScaffold(notes = "This is some details")
            TaskRowScaffold(notes = "This is some details about the task\nsdfsdfsd sdfsdf sdf sdfsd f\ns fsdfsd fsdfdsf f\n", dueDate = null)
            TaskRowScaffold(title = "My completed task", notes = "This is some details", isCompleted = true)
        }
    }
}
