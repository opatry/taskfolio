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
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview

@PreviewLightDark
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
