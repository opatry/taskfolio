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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.LABEL
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.REMAINING_TASKS_COUNT_BADGE
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.REMAINING_TASKS_COUNT_BADGE_LABEL
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

@VisibleForTesting
internal object TaskListRowTestTag {
    const val LABEL = "TASK_LIST_LABEL"
    const val REMAINING_TASKS_COUNT_BADGE = "TASK_LIST_REMAINING_TASKS_COUNT_BADGE"
    const val REMAINING_TASKS_COUNT_BADGE_LABEL = "TASK_LIST_REMAINING_TASKS_COUNT_BADGE_LABEL"
}

@Composable
fun TaskListRow(
    taskList: TaskListUIModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSelected = taskList.isSelected
    val remainingTasksCount = taskList.allRemainingTasks.size
    val cellBackground = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .3f)
        else -> Color.Unspecified
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .semantics { selected = isSelected },
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = taskList.title,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(LABEL),
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1,
                    )
                    AnimatedVisibility(
                        visible = !isSelected && remainingTasksCount > 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        Badge(
                            modifier = Modifier.testTag(REMAINING_TASKS_COUNT_BADGE),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                text = when {
                                    remainingTasksCount > 999 -> "999+"
                                    else -> remainingTasksCount.toString()
                                },
                                modifier = Modifier.testTag(REMAINING_TASKS_COUNT_BADGE_LABEL)
                            )
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = cellBackground
            )
        )
    }
}

private class TaskListSimplePreviewParameterProvider : PreviewParameterProvider<TaskListUIModel> {
    override val values: Sequence<TaskListUIModel>
        get() = sequenceOf(
            TaskListUIModel(
                id = TaskListId(0L),
                title = "My task list",
            ),
            TaskListUIModel(
                id = TaskListId(0L),
                title = "My selected task list",
                isSelected = true,
            ),
            TaskListUIModel(
                id = TaskListId(0L),
                title = "This is a task list with a very very very long name",
            ),
            TaskListUIModel(
                id = TaskListId(0L),
                title = "This is a task list with a very very very long name",
                remainingTasks = mapOf(
                    null to listOf(
                        TaskUIModel.Todo(
                            id = TaskId(0L),
                            title = "Task 1",
                        ),
                    ),
                )
            ),
            TaskListUIModel(
                id = TaskListId(0L),
                title = "This is a task list with a very very very long name",
                remainingTasks = mapOf(
                    null to List(1500) {
                        TaskUIModel.Todo(
                            id = TaskId(it.toLong()),
                            title = "Task $it",
                        )
                    }
                )
            ),
        )
}

@Preview
@Composable
private fun TaskListRowPreview(
    @PreviewParameter(TaskListSimplePreviewParameterProvider::class)
    taskList: TaskListUIModel
) {
    TaskfolioThemedPreview {
        TaskListRow(taskList) {}
    }
}
