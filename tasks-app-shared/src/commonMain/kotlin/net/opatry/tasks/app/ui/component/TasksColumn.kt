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

import CheckCheck
import ChevronDown
import ChevronRight
import LucideIcons
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.ALL_COMPLETE_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE_LABEL
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.TASKS_COLUMN
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_pane_all_tasks_complete_desc
import net.opatry.tasks.resources.task_list_pane_all_tasks_complete_title
import net.opatry.tasks.resources.task_list_pane_completed_section_title_with_count
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting
internal object TasksColumnTestTag {
    const val ALL_COMPLETE_EMPTY_STATE = "ALL_COMPLETE_EMPTY_STATE"
    const val TASKS_COLUMN = "TASKS_COLUMN"
    const val COMPLETED_TASKS_TOGGLE = "COMPLETED_TASKS_TOGGLE"
    const val COMPLETED_TASKS_TOGGLE_LABEL = "COMPLETED_TASKS_TOGGLE_LABEL"
}

@Composable
fun TasksColumn(
    taskLists: List<TaskListUIModel>,
    taskList: TaskListUIModel,
    onDeleteList: () -> Unit = {},
    onRepairList: () -> Unit = {},
    onTaskAction: (TaskAction) -> Unit = {},
    showCompletedDefaultValue: Boolean = false,
) {
    var showCompleted by remember(taskList.id) { mutableStateOf(showCompletedDefaultValue) }

    when {
        taskList.isEmpty -> {
            NoTasksEmptyState()
        }

        taskList.hasBrokenIndentation() -> {
            BrokenListIndentationEmptyState(
                onDeleteList = onDeleteList,
                onRepairList = onRepairList,
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.testTag(TASKS_COLUMN),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (taskList.isEmptyRemainingTasksVisible) {
                    item(key = "all_tasks_complete") {
                        EmptyState(
                            icon = LucideIcons.CheckCheck,
                            title = stringResource(Res.string.task_list_pane_all_tasks_complete_title),
                            description = stringResource(Res.string.task_list_pane_all_tasks_complete_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag(ALL_COMPLETE_EMPTY_STATE),
                        )
                    }
                }

                taskList.remainingTasks.forEach { (dateRange, tasks) ->
                    if (dateRange != null) {
                        stickyHeader(key = dateRange.key) {
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
                    items(tasks, key = { it.id.value }) { task ->
                        RemainingTaskRow(
                            taskLists,
                            task,
                            // TODO could come from the UI mapper/UI state
                            showDate = when {
                                taskList.sorting == TaskListSorting.Manual -> true
                                taskList.sorting == TaskListSorting.Title -> true
                                dateRange is DateRange.Overdue -> true
                                else -> false
                            },
                            onAction = onTaskAction,
                        )
                    }
                }

                if (taskList.hasCompletedTasks) {
                    stickyHeader(key = "completed") {
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.large)
                                .fillMaxWidth()
                                .clickable { showCompleted = !showCompleted }
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag(COMPLETED_TASKS_TOGGLE)
                        ) {
                            RowWithIcon(
                                icon = {
                                    when {
                                        showCompleted -> Icon(LucideIcons.ChevronDown, null)
                                        else -> Icon(LucideIcons.ChevronRight, null)
                                    }
                                }
                            ) {
                                Text(
                                    stringResource(Res.string.task_list_pane_completed_section_title_with_count, taskList.completedTasks.size),
                                    modifier = Modifier.testTag(COMPLETED_TASKS_TOGGLE_LABEL),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }

                if (showCompleted) {
                    items(taskList.completedTasks, key = { it.id.value }) { task ->
                        CompletedTaskRow(
                            task,
                            onAction = onTaskAction,
                        )
                    }
                }
            }
        }
    }
}

private val DateRange.key: String
    get() = when (this) {
        is DateRange.Overdue -> "overdue${numberOfDays}"
        is DateRange.Today -> "today"
        is DateRange.Later -> "later${numberOfDays}"
        DateRange.None -> "none"
    }

