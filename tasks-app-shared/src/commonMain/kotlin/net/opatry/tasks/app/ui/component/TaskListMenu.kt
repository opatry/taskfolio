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

import Check
import EllipsisVertical
import LucideIcons
import Trash2
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.screen.TaskListSorting
import net.opatry.tasks.app.ui.tooling.TaskfolioPreview
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_menu_clear_all_completed_tasks
import net.opatry.tasks.resources.task_list_menu_default_list_cannot_be_deleted
import net.opatry.tasks.resources.task_list_menu_delete
import net.opatry.tasks.resources.task_list_menu_rename
import net.opatry.tasks.resources.task_list_menu_sort_by
import net.opatry.tasks.resources.task_list_menu_sort_due_date
import net.opatry.tasks.resources.task_list_menu_sort_manual
import org.jetbrains.compose.resources.stringResource

enum class TaskListMenuAction {
    Dismiss,
    SortManual,
    SortDate,
    Rename,
    ClearCompletedTasks,
    Delete,
}

@Composable
fun TaskListMenu(
    taskList: TaskListUIModel,
    expanded: Boolean,
    sorting: TaskListSorting = TaskListSorting.Manual,
    onAction: (TaskListMenuAction) -> Unit
) {
    val allowDelete by remember(taskList.canDelete) { mutableStateOf(taskList.canDelete) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onAction(TaskListMenuAction.Dismiss) }
    ) {
        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_sort_by), style = MaterialTheme.typography.titleSmall)
            },
            enabled = false,
            onClick = {}
        )

        DropdownMenuItem(
            text = {
                RowWithIcon(
                    stringResource(Res.string.task_list_menu_sort_manual),
                    LucideIcons.Check.takeIf { sorting == TaskListSorting.Manual })
            },
            enabled = sorting != TaskListSorting.Manual,
            onClick = { onAction(TaskListMenuAction.SortManual) }
        )

        DropdownMenuItem(
            text = {
                RowWithIcon(
                    stringResource(Res.string.task_list_menu_sort_due_date),
                    LucideIcons.Check.takeIf { sorting == TaskListSorting.DueDate })
            },
            enabled = sorting != TaskListSorting.DueDate,
            onClick = { onAction(TaskListMenuAction.SortDate) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_rename))
            },
            onClick = { onAction(TaskListMenuAction.Rename) }
        )

        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_clear_all_completed_tasks))
            },
            enabled = taskList.hasCompletedTasks,
            onClick = { onAction(TaskListMenuAction.ClearCompletedTasks) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                val color = when {
                    allowDelete -> MaterialTheme.colorScheme.error
                    else -> LocalContentColor.current
                }
                CompositionLocalProvider(LocalContentColor provides color) {
                    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowWithIcon(stringResource(Res.string.task_list_menu_delete), LucideIcons.Trash2)
                        if (!allowDelete) {
                            Text(
                                stringResource(Res.string.task_list_menu_default_list_cannot_be_deleted),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            enabled = allowDelete,
            onClick = { onAction(TaskListMenuAction.Delete) }
        )
    }
}

@TaskfolioPreview
@Composable
private fun TaskListMenuPreview() {
    var showMenu by remember { mutableStateOf(true) }
    TaskfolioThemedPreview {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp), contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = { showMenu = true }) {
                Icon(LucideIcons.EllipsisVertical, null)
                TaskListMenu(TaskListUIModel(0L, "My task list", "TODO DATE", tasks = emptyList()), showMenu) {}
            }
        }
    }
}