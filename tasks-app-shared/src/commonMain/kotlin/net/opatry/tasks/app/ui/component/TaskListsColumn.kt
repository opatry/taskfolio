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

import CircleFadingPlus
import LucideIcons
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskListsPaneTestTag.NEW_TASK_LIST_BUTTON
import net.opatry.tasks.app.ui.component.TaskListsPaneTestTag.TASK_LIST_ROW
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_add_task_list_cta
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview


@VisibleForTesting
internal object TaskListsPaneTestTag {
    const val NEW_TASK_LIST_BUTTON = "NEW_TASK_LIST_BUTTON"
    const val TASK_LIST_ROW = "TASK_LIST_ROW"
}

@Composable
fun TaskListsColumn(
    taskLists: List<TaskListUIModel>,
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
        items(taskLists, { it.id.value }) { taskList ->
            TaskListRow(
                taskList,
                Modifier
                    .testTag(TASK_LIST_ROW)
                    .padding(horizontal = 8.dp)
                    .animateItem(),
                onClick = { onItemClick(taskList) }
            )
        }
    }
}

@Preview
@Composable
private fun TaskListsColumnPreview() {
    val taskLists = listOf(
        TaskListUIModel(
            id = TaskListId(0L),
            title = "My task list",
            remainingTasks = mapOf(
                null to List(12) {
                    TaskUIModel.Todo(
                        id = TaskId(it.toLong()),
                        title = "Task $it",
                    )
                },
            )
        ),
        TaskListUIModel(
            id = TaskListId(1L),
            title = "My selected task list",
            isSelected = true,
        ),
        TaskListUIModel(
            id = TaskListId(2L),
            title = "This is a task list with a very very very long name",
        ),
        TaskListUIModel(
            id = TaskListId(3L),
            title = "This with remaining task count",
            remainingTasks = mapOf(
                null to List(1500) {
                    TaskUIModel.Todo(
                        id = TaskId(it.toLong()),
                        title = "Task $it",
                    )
                },
            )
        ),
    )

    TaskfolioThemedPreview {
        TaskListsColumn(taskLists, {}, {})
    }
}
