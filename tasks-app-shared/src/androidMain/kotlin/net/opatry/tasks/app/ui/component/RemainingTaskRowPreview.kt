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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview


private class RemainingTaskRowPreviewDataProvider :
    PreviewParameterProvider<TaskUIModel> {
    override val values = sequenceOf(
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "Without due date",
            dueDate = null,
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With notes",
            notes = "Some notes\nthat are long enough\nto be wrapped on multiple lines",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With very very very very very very very very long title",
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date in the past",
            dueDate = Clock.System.todayIn(TimeZone.UTC).minus(3, DateTimeUnit.MONTH),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date today",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date yesterday",
            dueDate = Clock.System.todayIn(TimeZone.UTC).minus(1, DateTimeUnit.DAY),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date tomorrow",
            dueDate = Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.DAY),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date in distant future",
            dueDate = Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.MONTH),
        ),
    )
}

@PreviewLightDark
@Composable
private fun CompletedTaskRowPreview(
    @PreviewParameter(RemainingTaskRowPreviewDataProvider::class)
    task: TaskUIModel.Todo,
) {
    TaskfolioThemedPreview {
        RemainingTaskRow(
            taskLists = emptyList(),
            task = task,
            showDate = true,
            onAction = {}
        )
    }
}