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
import CopyPlus
import ListPlus
import LucideIcons
import SquareStack
import Trash2
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
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.ADD_SUBTASK
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.INDENT
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_NEW_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_TOP
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.TASK_MENU
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.UNINDENT
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

object TaskMenuTestTag {
    const val TASK_MENU = "TASK_MENU"
    const val ADD_SUBTASK = "TASK_MENU_ADD_SUBTASK"
    const val MOVE_TO_TOP = "MOVE_TO_TOP"
    const val UNINDENT = "UNINDENT"
    const val INDENT = "INDENT"
    const val MOVE_TO_LIST = "MOVE_TO_LIST"
    const val MOVE_TO_NEW_LIST = "MOVE_TO_NEW_LIST"
    const val DELETE = "DELETE"
}

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
    val currentTaskList = taskLists.firstOrNull { it.containsTask(task) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onAction(null) },
        modifier = Modifier.testTag(TASK_MENU)
    ) {
        if (task.canMoveToTop) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_move_to_top))
                },
                onClick = { onAction(TaskAction.MoveToTop) },
                modifier = Modifier.testTag(MOVE_TO_TOP),
            )
        }

        if (task.canCreateSubTask) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_add_subtask), LucideIcons.CopyPlus)
                },
                onClick = { onAction(TaskAction.AddSubTask) },
                modifier = Modifier.testTag(ADD_SUBTASK),
            )
        }

        if (task.canIndent) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_indent))
                },
                onClick = { onAction(TaskAction.Indent) },
                modifier = Modifier.testTag(INDENT),
                enabled = false // TODO support indentation
            )
        }

        if (task.canUnindent) {
            DropdownMenuItem(
                text = {
                    RowWithIcon(stringResource(Res.string.task_menu_unindent))
                },
                onClick = { onAction(TaskAction.Unindent) },
                modifier = Modifier.testTag(UNINDENT),
                enabled = false // TODO support indentation
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(stringResource(Res.string.task_menu_move_to), style = MaterialTheme.typography.titleSmall)
            },
            enabled = false, // TODO support task move to list
            onClick = {}
        )

        DropdownMenuItem(
            text = {
                RowWithIcon(stringResource(Res.string.task_menu_new_list), LucideIcons.ListPlus)
            },
            onClick = { onAction(TaskAction.MoveToNewList) },
            modifier = Modifier.testTag(MOVE_TO_NEW_LIST),
        )

        // FIXME not ideal when a lot of list, maybe ask for a dialog or bottom sheet in which to choose?
        //  or using a submenu?
        val enableMoveTaskList = true // TODO should it be hidden when 1 list only?
        if (enableMoveTaskList) {
            taskLists.forEach { taskList ->
                val isCurrentList = taskList.id == currentTaskList?.id
                DropdownMenuItem(
                    text = {
                        RowWithIcon(
                            icon = LucideIcons.Check.takeIf { isCurrentList },
                            text = taskList.title,
                        )
                    },
                    modifier = Modifier.testTag(MOVE_TO_LIST),
                    enabled = !isCurrentList,
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
            modifier = Modifier.testTag(DELETE),
            onClick = { onAction(TaskAction.Delete) }
        )
    }
}