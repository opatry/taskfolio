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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.component.NoTaskListEmptyState
import net.opatry.tasks.app.ui.component.NoTaskListSelectedEmptyState
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_default_task_list_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun TaskListsMasterDetail(
    viewModel: TaskListsViewModel,
    onNewTaskList: (String) -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsState(null)

    // need to store a saveable (Serializable/Parcelable) object
    // rememberListDetailPaneScaffoldNavigator, under the hood uses rememberSaveable with it
    // we use the TaskListUIModel.id as the key to save the state of the navigator
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            val list = taskLists
            AnimatedPane {
                when {
                    list == null -> LoadingPane()

                    list.isEmpty() -> {
                        val newTaskListName = stringResource(Res.string.task_lists_screen_default_task_list_title)
                        NoTaskListEmptyState {
                            onNewTaskList(newTaskListName)
                        }
                    }

                    else -> Row {
                        TaskListsColumn(
                            list,
                            selectedItem = list.find { it.id == navigator.currentDestination?.content },
                            onNewTaskList = { onNewTaskList("") },
                            onItemClick = { taskList ->
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taskList.id)
                            }
                        )

                        if (navigator.scaffoldValue.primary == PaneAdaptedValue.Expanded) {
                            VerticalDivider()
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val list = taskLists
                val selectedList = list?.find { it.id == navigator.currentDestination?.content }
                when {
                    list == null -> LoadingPane()
                    selectedList == null -> NoTaskListSelectedEmptyState()
                    else -> TaskListDetail(viewModel, selectedList) { targetedTaskList ->
                        when (targetedTaskList) {
                            null -> navigator.navigateBack()
                            else -> navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, targetedTaskList.id)
                        }
                    }
                }
            }
        }
    )
}