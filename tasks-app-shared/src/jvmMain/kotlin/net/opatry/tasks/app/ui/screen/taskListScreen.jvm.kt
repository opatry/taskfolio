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

package net.opatry.tasks.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.component.NoTaskListEmptyState
import net.opatry.tasks.app.ui.component.NoTaskListSelectedEmptyState
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_default_task_list_title
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun TaskListsMasterDetail(
    viewModel: TaskListsViewModel,
    onNewTaskList: (String) -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsState(null)

    // Store the list id, and not the list object to prevent keeping
    // a stale object when data changes.
    var currentTaskListId by remember { mutableStateOf<Long?>(null) }

    Row(Modifier.fillMaxWidth()) {
        val list = taskLists
        when {
            list == null -> LoadingPane()

            list.isEmpty() -> {
                val newTaskListName = stringResource(Res.string.task_lists_screen_default_task_list_title)
                NoTaskListEmptyState {
                    onNewTaskList(newTaskListName)
                }
            }

            else -> {
                Box(Modifier.weight(.3f)) {
                    TaskListsColumn(
                        list,
                        selectedItem = list.find { it.id == currentTaskListId },
                        onNewTaskList = { onNewTaskList("") },
                        onItemClick = { taskList ->
                            currentTaskListId = taskList.id
                        }
                    )
                }

                VerticalDivider()
            }
        }

        Box(Modifier.weight(.7f)) {
            val selectedList = list?.find { it.id == currentTaskListId }
            when {
                list == null -> LoadingPane()
                selectedList == null -> NoTaskListSelectedEmptyState()
                else -> TaskListDetail(viewModel, selectedList) { targetedTaskList ->
                    currentTaskListId = targetedTaskList?.id
                }
            }
        }
    }
}