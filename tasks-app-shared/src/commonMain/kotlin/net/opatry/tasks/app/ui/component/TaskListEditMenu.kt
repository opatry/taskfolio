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

import EllipsisVertical
import LucideIcons
import Trash2
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.CLEAR_COMPLETED_TASKS
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.DELETE_NOT_ALLOWED_NOTICE
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.EDIT_MENU
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.RENAME
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_menu_clear_all_completed_tasks
import net.opatry.tasks.resources.task_list_menu_default_list_cannot_be_deleted
import net.opatry.tasks.resources.task_list_menu_delete
import net.opatry.tasks.resources.task_list_menu_rename
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@VisibleForTesting
internal object TaskListEditMenuTestTag {
    const val EDIT_MENU = "TASK_LIST_EDIT_MENU"
    const val RENAME = "TASK_LIST_RENAME"
    const val CLEAR_COMPLETED_TASKS = "TASK_LIST_CLEAR_COMPLETED_TASKS"
    const val DELETE = "TASK_LIST_DELETE"
    const val DELETE_NOT_ALLOWED_NOTICE = "TASK_LIST_DELETE_NOT_ALLOWED_NOTICE"
}

enum class TaskListEditMenuAction {
    Rename,
    ClearCompletedTasks,
    Delete,
}

@Composable
fun TaskListEditMenu(
    taskList: TaskListUIModel,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (TaskListEditMenuAction) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(EDIT_MENU),
    ) {
        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_rename))
            },
            onClick = { onAction(TaskListEditMenuAction.Rename) },
            modifier = Modifier.testTag(RENAME),
        )

        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_list_menu_clear_all_completed_tasks))
            },
            enabled = taskList.hasCompletedTasks,
            onClick = { onAction(TaskListEditMenuAction.ClearCompletedTasks) },
            modifier = Modifier.testTag(CLEAR_COMPLETED_TASKS),
        )

        HorizontalDivider()

        val isDeletedAllowed = taskList.canDelete
        DropdownMenuItem(
            text = {
                val color = when {
                    isDeletedAllowed -> MaterialTheme.colorScheme.error
                    else -> LocalContentColor.current
                }
                CompositionLocalProvider(LocalContentColor provides color) {
                    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowWithIcon(stringResource(Res.string.task_list_menu_delete), LucideIcons.Trash2)
                        if (!isDeletedAllowed) {
                            Text(
                                text = stringResource(Res.string.task_list_menu_default_list_cannot_be_deleted),
                                modifier = Modifier.testTag(DELETE_NOT_ALLOWED_NOTICE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            enabled = isDeletedAllowed,
            onClick = { onAction(TaskListEditMenuAction.Delete) },
            modifier = Modifier.testTag(DELETE),
        )
    }
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
                Icon(LucideIcons.EllipsisVertical, null)
                TaskListEditMenu(
                    taskList = TaskListUIModel(
                        id = TaskListId(0L),
                        title = "My task list",
                    ),
                    expanded = true,
                    onDismiss = {},
                    onAction = {},
                )
            }
        }
    }
}