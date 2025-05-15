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
import kotlinx.datetime.LocalDate
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.data.TaskListSorting

private class TaskListPreviewParameterProvider : PreviewParameterProvider<TaskListUIModel> {
    override val values = sequenceOf(
        // fully empty state
        TaskListUIModel(
            id = TaskListId(0L),
            title = "Whole new list",
            remainingTasks = emptyMap(),
            completedTasks = emptyList()
        ),
        // all done empty state
        TaskListUIModel(
            id = TaskListId(0L),
            title = "All done",
            remainingTasks = emptyMap(),
            completedTasks = listOf(
                TaskUIModel(
                    id = TaskId(1L),
                    title = "Task 1",
                    isCompleted = true,
                ),
            )
        ),
        // remaining tasks sorted manually
        TaskListUIModel(
            id = TaskListId(0L),
            title = "All remains manually ordered",
            sorting = TaskListSorting.Manual,
            remainingTasks = mapOf(
                DateRange.None to listOf(
                    TaskUIModel(
                        id = TaskId(1L),
                        title = "Task 1",
                        isCompleted = false,
                    ),
                ),
                DateRange.Overdue(LocalDate.parse("2023-01-01"), 40) to listOf(
                    TaskUIModel(
                        id = TaskId(2L),
                        title = "Task 2",
                        isCompleted = false,
                    ),
                    TaskUIModel(
                        id = TaskId(3L),
                        title = "Task 3",
                        isCompleted = false,
                        indent = 1,
                    ),
                ),
            ),
            completedTasks = emptyList()
        ),
    )
}

@PreviewLightDark
@Composable
private fun TasksColumnPreview(
    @PreviewParameter(TaskListPreviewParameterProvider::class)
    taskList: TaskListUIModel,
) {
    TaskfolioThemedPreview {
        TasksColumn(
            taskLists = listOf(taskList),
            taskList = taskList,
            showCompletedDefaultValue = true
        )
    }
}