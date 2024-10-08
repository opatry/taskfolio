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
import ListPlus
import LucideIcons
import SquareStack
import Trash2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_menu_add_subtask
import net.opatry.tasks.resources.task_menu_delete
import net.opatry.tasks.resources.task_menu_indent
import net.opatry.tasks.resources.task_menu_move_to
import net.opatry.tasks.resources.task_menu_move_to_top
import net.opatry.tasks.resources.task_menu_new_list
import net.opatry.tasks.resources.task_menu_unindent
import org.jetbrains.compose.resources.stringResource

sealed class TaskAction {
    data object ToggleCompletion : TaskAction()
    data object Edit : TaskAction()
    data object UpdateDueDate : TaskAction()
    data object AddSubTask : TaskAction()
    data object MoveToTop : TaskAction()
    data object Unindent : TaskAction()
    data object Indent : TaskAction()
    data class MoveToList(val targetParentList: TaskListUIModel) : TaskAction()
    data object MoveToNewList : TaskAction()
    data object Delete : TaskAction()
}

@Composable
fun TaskMenu(
    taskLists: List<TaskListUIModel>,
    task: TaskUIModel,
    expanded: Boolean,
    onAction: (TaskAction?) -> Unit
) {
    val currentTaskList = taskLists.firstOrNull { it.tasks.map(TaskUIModel::id).contains(task.id) }
    val taskPosition by remember(currentTaskList) { mutableStateOf(currentTaskList?.tasks?.indexOf(task) ?: -1) }
    val canMoveToTop by remember(task) { derivedStateOf { taskPosition > 0 && task.canIndent } }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onAction(null) }
    ) {
        if (canMoveToTop) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_move_to_top))
                },
                onClick = { onAction(TaskAction.MoveToTop) },
                enabled = false
            )
        }

        if (task.canCreateSubTask) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_add_subtask), LucideIcons.SquareStack)
                },
                onClick = { onAction(TaskAction.AddSubTask) },
                enabled = false
            )
        }

        if (task.canIndent && taskPosition > 0) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_indent))
                },
                onClick = { onAction(TaskAction.Indent) },
                enabled = false
            )
        }

        if (task.canUnindent) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_unindent))
                },
                onClick = { onAction(TaskAction.Unindent) },
                enabled = false
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_menu_move_to), style = MaterialTheme.typography.titleSmall)
            },
            enabled = false,
            onClick = {}
        )

        DropdownMenuItem(
            text = {
                RowWithIcon(stringResource(Res.string.task_menu_new_list), LucideIcons.ListPlus)
            },
            onClick = { onAction(TaskAction.MoveToNewList) },
            enabled = false,
        )

        // FIXME not ideal when a lot of list, maybe ask for a dialog or bottom sheet in which to choose?
        //  or using a submenu?
        val enableMoveTaskList = false
        if (enableMoveTaskList) {
            taskLists.forEach { taskList ->
                DropdownMenuItem(
                    text = {
                        RowWithIcon(icon = if (taskList.id == currentTaskList?.id) {
                            { Icon(LucideIcons.Check, null) }
                        } else null
                        ) {
                            Text(taskList.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }
                    },
                    enabled = taskList.id != currentTaskList?.id,
                    onClick = { onAction(TaskAction.MoveToList(taskList)) }
                )
            }
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                    RowWithIcon(stringResource(Res.string.task_menu_delete), LucideIcons.Trash2)
                }
            },
            onClick = { onAction(TaskAction.Delete) }
        )
    }
}