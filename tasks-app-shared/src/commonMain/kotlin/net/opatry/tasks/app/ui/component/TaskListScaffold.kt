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

package net.opatry.tasks.app.ui.component

import LucideIcons
import Plus
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskListScaffoldTestTag.ADD_TASK_FAB
import net.opatry.tasks.data.TaskListSorting

@VisibleForTesting
internal object TaskListScaffoldTestTag {
    const val ADD_TASK_FAB = "TASK_LIST_SCAFFOLD_ADD_TASK_FAB"
}

@Composable
fun TaskListScaffold(
    taskLists: List<TaskListUIModel>,
    taskList: TaskListUIModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onListSort: (TaskListSorting) -> Unit = {},
    onListEdit: (TaskListEditMenuAction) -> Unit = {},
    onNewTask: () -> Unit = {},
    onDeleteList: () -> Unit = {},
    onRepairList: () -> Unit = {},
    onToggleTaskCompletionState: (TaskUIModel) -> Unit = {},
    onEditTask: (TaskUIModel) -> Unit = {},
    onUpdateTaskDueDate: (TaskUIModel) -> Unit = {},
    onNewSubTask: (TaskUIModel) -> Unit = {},
    onUnindentTask: (TaskUIModel) -> Unit = {},
    onIndentTask: (TaskUIModel) -> Unit = {},
    onMoveTaskToTop: (TaskUIModel) -> Unit = {},
    onMoveTaskToList: (TaskUIModel, TaskListUIModel) -> Unit = { _, _ -> },
    onMoveTaskToNewList: (TaskUIModel) -> Unit = {},
    onDeleteTask: (TaskUIModel) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TaskListTopAppBar(taskList, onListSort, onListEdit)
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        // FIXME should be driven by the NavigationRail
        floatingActionButton = {
            if (!taskList.hasBrokenIndentation()) {
                // FIXME hides bottom of screen
                FloatingActionButton(
                    onClick = onNewTask,
                    modifier = Modifier.testTag(ADD_TASK_FAB)
                ) {
                    Icon(LucideIcons.Plus, null)
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            TasksColumn(
                taskLists = taskLists,
                taskList = taskList,
                onDeleteList = onDeleteList,
                onRepairList = onRepairList,
                onToggleTaskCompletionState = onToggleTaskCompletionState,
                onEditTask = onEditTask,
                onUpdateTaskDueDate = onUpdateTaskDueDate,
                onNewSubTask = onNewSubTask,
                onUnindentTask = onUnindentTask,
                onIndentTask = onIndentTask,
                onMoveTaskToTop = onMoveTaskToTop,
                onMoveTaskToList = onMoveTaskToList,
                onMoveTaskToNewList = onMoveTaskToNewList,
                onDeleteTask = onDeleteTask,
            )
        }
    }
}