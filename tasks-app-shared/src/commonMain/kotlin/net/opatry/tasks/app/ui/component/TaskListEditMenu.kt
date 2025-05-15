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
import Trash2
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.CLEAR_COMPLETED_TASKS
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.EDIT_MENU
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.RENAME
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_menu_clear_all_completed_tasks
import net.opatry.tasks.resources.task_list_menu_default_list_cannot_be_deleted
import net.opatry.tasks.resources.task_list_menu_delete
import net.opatry.tasks.resources.task_list_menu_rename
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting
internal object TaskListEditMenuTestTag {
    const val EDIT_MENU = "TASK_LIST_EDIT_MENU"
    const val RENAME = "TASK_LIST_RENAME"
    const val CLEAR_COMPLETED_TASKS = "TASK_LIST_CLEAR_COMPLETED_TASKS"
    const val DELETE = "TASK_LIST_DELETE"
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
                                stringResource(Res.string.task_list_menu_default_list_cannot_be_deleted),
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
