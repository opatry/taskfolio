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

import ArrowDownAZ
import Check
import LucideIcons
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_DUE_DATE
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_MANUAL
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_MENU
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_TITLE
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_menu_sort_by
import net.opatry.tasks.resources.task_list_menu_sort_due_date
import net.opatry.tasks.resources.task_list_menu_sort_manual
import net.opatry.tasks.resources.task_list_menu_sort_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@VisibleForTesting
internal object TaskListSortMenuTestTag {
    const val SORT_MENU = "TASK_LIST_SORT_MENU"
    const val SORT_MANUAL = "TASK_LIST_SORT_MANUAL"
    const val SORT_DUE_DATE = "TASK_LIST_SORT_DUE_DATE"
    const val SORT_TITLE = "TASK_LIST_SORT_TITLE"
}

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
        modifier = Modifier.testTag(SORT_MENU)
    ) {
        MenuSectionItem(Res.string.task_list_menu_sort_by)

        // using listOf to enforce specific order and avoid relying on Enum::entries order
        listOf(
            TaskListSorting.Manual,
            TaskListSorting.DueDate,
            TaskListSorting.Title,
        ).forEach { sorting ->
            SortMenuItem(sorting, taskList.sorting) { onSortingClick(sorting) }
        }
    }
}

@Composable
private fun MenuSectionItem(labelRes: StringResource) {
    MenuSectionItem(stringResource(labelRes))
}

@Composable
private fun MenuSectionItem(label: String) {
    DropdownMenuItem(
        text = {
            Text(label, style = MaterialTheme.typography.titleSmall)
        },
        enabled = false,
        onClick = {}
    )
}

private val TaskListSorting.labelRes: StringResource
    get() = when (this) {
        TaskListSorting.Manual -> Res.string.task_list_menu_sort_manual
        TaskListSorting.DueDate -> Res.string.task_list_menu_sort_due_date
        TaskListSorting.Title -> Res.string.task_list_menu_sort_title
    }

private val TaskListSorting.testTag: String
    get() = when (this) {
        TaskListSorting.Manual -> SORT_MANUAL
        TaskListSorting.DueDate -> SORT_DUE_DATE
        TaskListSorting.Title -> SORT_TITLE
    }

@Composable
private fun SortMenuItem(
    sorting: TaskListSorting,
    currentSorting: TaskListSorting,
    onClick: () -> Unit
) {
    val isCurrentSorting = sorting == currentSorting
    DropdownMenuItem(
        text = {
            RowWithIcon(
                stringResource(sorting.labelRes),
                LucideIcons.Check.takeIf { isCurrentSorting }
            )
        },
        enabled = !isCurrentSorting,
        onClick = onClick,
        modifier = Modifier.testTag(sorting.testTag)
    )
}

@Preview
@Composable
private fun TaskListMenuPreview() {
    TaskfolioThemedPreview {
        Box(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(24.dp), contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = {}) {
                Icon(LucideIcons.ArrowDownAZ, null)
                TaskListSortMenu(
                    taskList = TaskListUIModel(
                        id = TaskListId(0L),
                        title = "My task list",
                        sorting = TaskListSorting.Title,
                    ),
                    expanded = true,
                    onDismiss = {},
                    onSortingClick = {},
                )
            }
        }
    }
}