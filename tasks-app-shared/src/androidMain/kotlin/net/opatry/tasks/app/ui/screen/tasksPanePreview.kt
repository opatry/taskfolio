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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.ui.component.CompletedTaskRow
import net.opatry.tasks.app.ui.component.RemainingTaskRow
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview


@Composable
private fun TaskRowScaffold(
    title: String = "My task",
    notes: String = "",
    dueDate: LocalDate? = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    isCompleted: Boolean = false
) {
    if (isCompleted) {
        CompletedTaskRow(
            TaskUIModel(
                id = TaskId(0L),
                title = title,
                notes = notes,
                dueDate = dueDate,
                isCompleted = true
            )
        ) {}
    } else {
        RemainingTaskRow(
            emptyList(),
            TaskUIModel(
                id = TaskId(0L),
                title = title,
                notes = notes,
                dueDate = dueDate,
                isCompleted = false
            )
        ) {}
    }
}

@PreviewLightDark
@Composable
private fun TaskRowDatesPreview() {
    TaskfolioThemedPreview {
        Column {
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(180, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(3, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(1, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(1, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(10, DateTimeUnit.DAY))
            TaskRowScaffold(dueDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(500, DateTimeUnit.DAY))
        }
    }
}

@PreviewLightDark
@Composable
private fun TaskRowValuesPreview() {
    TaskfolioThemedPreview {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskRowScaffold(dueDate = null)
            TaskRowScaffold(notes = "This is some details")
            TaskRowScaffold(title = "A task with a very very very very long name", notes = "This is some details")
            TaskRowScaffold(notes = "This is some details about the task\nsdfsdfsd sdfsdf sdf sdfsd f\ns fsdfsd fsdfdsf f\n", dueDate = null)
            TaskRowScaffold(title = "A completed task with a very very very very long name", notes = "This is some details", isCompleted = true)
            TaskRowScaffold(title = "My completed task", notes = "This is some details", isCompleted = true)
        }
    }
}
