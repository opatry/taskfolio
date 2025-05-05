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

package net.opatry.tasks.app.ui.screen

import CircleFadingPlus
import LucideIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.component.RowWithIcon
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.screen.TaskListsPaneTestTag.NEW_TASK_LIST_BUTTON
import net.opatry.tasks.app.ui.screen.TaskListsPaneTestTag.TASK_LIST_ROW
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_add_task_list_cta
import org.jetbrains.compose.resources.stringResource


object TaskListsPaneTestTag {
    const val NEW_TASK_LIST_BUTTON = "NEW_TASK_LIST_BUTTON"
    const val TASK_LIST_ROW = "TASK_LIST_ROW"
}

@Composable
fun TaskListsColumn(
    taskLists: List<TaskListUIModel>,
    selectedItem: TaskListUIModel? = null,
    onNewTaskList: () -> Unit,
    onItemClick: (TaskListUIModel) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stickyHeader(key = "new_task_list") {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // TODO could be a "in-place" replace with a text field (no border)
                TextButton(
                    onClick = onNewTaskList,
                    Modifier.testTag(NEW_TASK_LIST_BUTTON)
                ) {
                    RowWithIcon(stringResource(Res.string.task_lists_screen_add_task_list_cta), LucideIcons.CircleFadingPlus)
                }
            }

            AnimatedVisibility(listState.firstVisibleItemScrollOffset > 0) {
                HorizontalDivider()
            }
        }
        items(taskLists, TaskListUIModel::id) { taskList ->
            TaskListRow(
                taskList,
                Modifier.padding(horizontal = 8.dp).animateItem(),
                isSelected = taskList.id == selectedItem?.id,
                onClick = { onItemClick(taskList) }
            )
        }
    }
}

@Composable
fun TaskListRow(
    taskList: TaskListUIModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val cellBackground = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .3f)
        else -> Color.Unspecified
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .testTag(TASK_LIST_ROW)
            .semantics { selected = isSelected },
    ) {
        ListItem(
            headlineContent = {
                Text(taskList.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
            },
            colors = ListItemDefaults.colors(
                containerColor = cellBackground
            )
        )
    }
}
