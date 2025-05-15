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

import Check
import LucideIcons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_menu_sort_by
import net.opatry.tasks.resources.task_list_menu_sort_due_date
import net.opatry.tasks.resources.task_list_menu_sort_manual
import net.opatry.tasks.resources.task_list_menu_sort_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun TaskListSortMenu(
    taskList: TaskListUIModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSortingClick: (TaskListSorting) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_sort_by), style = MaterialTheme.typography.titleSmall)
            },
            enabled = false,
            onClick = {}
        )

        val isManualSorting = taskList.sorting == TaskListSorting.Manual
        DropdownMenuItem(
            text = {
                RowWithIcon(
                    stringResource(Res.string.task_list_menu_sort_manual),
                    LucideIcons.Check.takeIf { isManualSorting }
                )
            },
            enabled = !isManualSorting,
            onClick = { onSortingClick(TaskListSorting.Manual) }
        )

        val isDueDateSorting = taskList.sorting == TaskListSorting.DueDate
        DropdownMenuItem(
            text = {
                RowWithIcon(
                    stringResource(Res.string.task_list_menu_sort_due_date),
                    LucideIcons.Check.takeIf { isDueDateSorting }
                )
            },
            enabled = !isDueDateSorting,
            onClick = { onSortingClick(TaskListSorting.DueDate) }
        )

        val isTitleSorting = taskList.sorting == TaskListSorting.Title
        DropdownMenuItem(
            text = {
                RowWithIcon(
                    stringResource(Res.string.task_list_menu_sort_title),
                    LucideIcons.Check.takeIf { isTitleSorting }
                )
            },
            enabled = !isTitleSorting,
            onClick = { onSortingClick(TaskListSorting.Title) }
        )
    }
}
