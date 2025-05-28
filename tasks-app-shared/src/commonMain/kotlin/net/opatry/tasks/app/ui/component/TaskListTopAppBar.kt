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
import CalendarArrowDown
import EllipsisVertical
import ListTree
import LucideIcons
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.MORE_MENU_ICON
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.SORT_MENU_ICON
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.TITLE
import net.opatry.tasks.data.TaskListSorting

@VisibleForTesting
object TaskListTopAppBarTestTag {
    const val TITLE = "TASK_LIST_PANE_TITLE"
    const val SORT_MENU_ICON = "SORT_MENU_ICON"
    const val MORE_MENU_ICON = "MORE_MENU_ICON"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopAppBar(
    taskList: TaskListUIModel,
    onSort: (TaskListSorting) -> Unit,
    onEdit: (TaskListEditMenuAction) -> Unit,
) {
    var expandSortMenu by remember { mutableStateOf(false) }
    var expandEditMenu by remember { mutableStateOf(false) }

    // FIXME tweak colors, elevation, etc.
    TopAppBar(
        title = {
            Text(
                text = taskList.title,
                modifier = Modifier.testTag(TITLE),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        },
        actions = {
            if (!taskList.hasBrokenIndentation()) {
                IconButton(
                    onClick = { expandSortMenu = true },
                    modifier = Modifier.testTag(SORT_MENU_ICON),
                ) {
                    Crossfade(taskList.sorting) { sorting ->
                        val sortIcon = when (sorting) {
                            TaskListSorting.Manual -> LucideIcons.ListTree
                            TaskListSorting.DueDate -> LucideIcons.CalendarArrowDown
                            TaskListSorting.Title -> LucideIcons.ArrowDownAZ
                        }
                        Icon(sortIcon, null)
                    }
                    TaskListSortMenu(
                        taskList,
                        expandSortMenu,
                        onDismiss = {
                            expandSortMenu = false
                        },
                        onSortingClick = { sorting ->
                            expandSortMenu = false
                            onSort(sorting)
                        }
                    )
                }
                IconButton(
                    onClick = { expandEditMenu = true },
                    modifier = Modifier.testTag(MORE_MENU_ICON),
                ) {
                    Icon(LucideIcons.EllipsisVertical, null)
                    TaskListEditMenu(
                        taskList,
                        expandEditMenu,
                        onDismiss = {
                            expandEditMenu = false
                        },
                        onAction = { action ->
                            expandEditMenu = false
                            onEdit(action)
                        }
                    )
                }
            }
        }
    )
}