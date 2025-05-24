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

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview

private class TaskListSimplePreviewParameterProvider(
) : PreviewParameterProvider<TaskListUIModel> {
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

@PreviewLightDark
@Composable
private fun TaskListRowPreview(
    @PreviewParameter(TaskListSimplePreviewParameterProvider::class)
    taskList: TaskListUIModel
) {
    TaskfolioThemedPreview {
        TaskListRow(taskList) {}
    }
}
