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
import net.opatry.tasks.app.ui.model.TaskId
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview

private class CompletedTaskRowPreviewDataProvider :
    PreviewParameterProvider<TaskUIModel> {
    override val values = sequenceOf(
        TaskUIModel(
            id = TaskId(0),
            title = "Completed without due date",
            dueDate = null,
            isCompleted = true,
        ),
        TaskUIModel(
            id = TaskId(0),
            title = "Completed with due date",
            dueDate = LocalDate.parse("2025-01-01"),
            isCompleted = true,
        ),
    )
}

@PreviewLightDark
@Composable
private fun CompletedTaskRowPreview(
    @PreviewParameter(CompletedTaskRowPreviewDataProvider::class) task: TaskUIModel,
) {
    TaskfolioThemedPreview {
        CompletedTaskRow(
            task = task,
            onAction = {}
        )
    }
}