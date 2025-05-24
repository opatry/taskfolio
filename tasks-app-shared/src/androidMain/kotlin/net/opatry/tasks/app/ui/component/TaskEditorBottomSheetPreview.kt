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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.data.toTaskPosition

private data class TaskEditorPreviewData(
    val editMode: TaskEditMode,
    val task: TaskUIModel?,
    val taskLists: List<TaskListUIModel> = emptyList(),
    val taskList: TaskListUIModel = TaskListUIModel(
        id = TaskListId(0),
        title = "osef",
    ),
)

private class TaskEditorPreviewParameterProvider : PreviewParameterProvider<TaskEditorPreviewData> {
    override val values: Sequence<TaskEditorPreviewData>
        get() = sequenceOf(
            TaskEditorPreviewData(
                TaskEditMode.NewTask,
                task = null,
                taskLists = emptyList(),
                taskList = TaskListUIModel(
                    id = TaskListId(0),
                    title = "TODO",
                ),
            ),
            TaskEditorPreviewData(
                editMode = TaskEditMode.Edit,
                task = TaskUIModel.Todo(
                    id = TaskId(0),
                    title = "My edited task",
                    dueDate = LocalDate(2018, 6, 23),
                    position = 0.toTaskPosition(),
                ),
                taskList = TaskListUIModel(
                    id = TaskListId(0),
                    title = "Current list",
                ),
            ),
            TaskEditorPreviewData(
                editMode = TaskEditMode.NewSubTask,
                task = null,
                taskList = TaskListUIModel(
                    id = TaskListId(0),
                    title = "Current list",
                ),
            )
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun TaskEditorBottomSheetPreview(
    @PreviewParameter(TaskEditorPreviewParameterProvider::class)
    previewData: TaskEditorPreviewData
) {
    TaskfolioThemedPreview {
        val sheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded
        )
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Task Editor demo ${previewData.editMode.name}") })
            }
        ) { padding ->
            Column(modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)) {
                Text("Some screen content", style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                Text("Task lists", style = MaterialTheme.typography.titleMedium)
                previewData.taskLists.forEach { taskList ->
                    Text(taskList.title)
                }
                HorizontalDivider()
                Text("Selected task list", style = MaterialTheme.typography.titleMedium)
                Text(previewData.taskList.title)

                // FIXME standard bottom sheet state hides the content

                TaskEditorBottomSheet(
                    editMode = previewData.editMode,
                    task = previewData.task,
                    allTaskLists = previewData.taskLists,
                    sheetState = sheetState,
                    selectedTaskList = previewData.taskList,
                    onDismiss = {},
                    onEditDueDate = {},
                    onValidate = { _, _, _, _ -> },
                )
            }
        }
    }
}