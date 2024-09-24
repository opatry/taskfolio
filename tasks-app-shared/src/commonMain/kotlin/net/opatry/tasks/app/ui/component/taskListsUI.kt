/*
 * Copyright (c) 2024 Olivier Patry
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.tooling.TasksAppPreview
import net.opatry.tasks.app.ui.tooling.TasksAppThemedPreview


@Composable
fun TaskListsColumn(
    taskLists: List<TaskListUIModel>,
    selectedItem: TaskListUIModel? = null,
    onItemClick: (TaskListUIModel) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(taskLists) { taskList ->
            TaskListRow(
                taskList,
                isSelected = taskList.id == selectedItem?.id,
                onClick = { onItemClick(taskList) }
            )
        }
    }
}

@Composable
fun TaskListRow(
    taskList: TaskListUIModel,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val cellBackground = when {
        isSelected -> LocalContentColor.current.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Card(colors = CardDefaults.outlinedCardColors(), onClick = onClick) {
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

@Composable
private fun TaskListRowScaffold(
    title: String = "My task list",
    isSelected: Boolean = false
) {
    TaskListRow(
        TaskListUIModel(
            id = 0L,
            title = title,
            "TODO DATE",
            tasks = emptyList(),
        ),
        isSelected,
        {}
    )
}

@TasksAppPreview
@Composable
private fun TaskRowValuesPreview() {
    TasksAppThemedPreview {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskListRowScaffold("This is a task list with a very very very long name")
            TaskListRowScaffold(isSelected = true)
        }
    }
}
