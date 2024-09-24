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
import net.opatry.tasks.app.ui.component.NoTaskListEmptyState
import net.opatry.tasks.app.ui.component.NoTaskListSelectedEmptyState
import net.opatry.tasks.app.ui.component.TaskListDetail
import net.opatry.tasks.app.ui.component.TaskListsColumn
import net.opatry.tasks.app.ui.model.TaskListUIModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun TaskListsMasterDetail(viewModel: TaskListsViewModel) {
    val taskLists by viewModel.taskLists.collectAsState(emptyList())

    val navigator = rememberListDetailPaneScaffoldNavigator<TaskListUIModel>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                if (taskLists.isEmpty()) {
                    NoTaskListEmptyState()
                } else {
                    Row {
                        TaskListsColumn(
                            taskLists,
                            selectedItem = navigator.currentDestination?.content,
                            onItemClick = { taskList ->
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taskList)
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
                navigator.currentDestination?.content?.let { taskList ->
                    TaskListDetail(viewModel, taskList) { targetedTaskList ->
                        when (targetedTaskList) {
                            null -> navigator.navigateBack()
                            else -> navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, targetedTaskList)
                        }
                    }
                } ?: run {
                    NoTaskListSelectedEmptyState()
                }
            }
        }
    )
}