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

import CloudOff
import LucideIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.opatry.network.networkStateFlow
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.ui.TaskEvent
import net.opatry.tasks.app.ui.asLabel
import net.opatry.tasks.app.ui.component.Banner
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.component.MyBackHandler
import net.opatry.tasks.app.ui.component.NoTaskListsEmptyState
import net.opatry.tasks.app.ui.component.TaskListsColumn
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.network_unavailable_message
import net.opatry.tasks.resources.task_lists_screen_default_task_list_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun TaskListsMasterDetail(
    viewModel: TaskListsViewModel,
    onNewTaskList: (String) -> Unit
) {
    val taskLists by viewModel.taskLists.collectAsStateWithLifecycle(null)
    val selectedTaskListId by viewModel.selectedTaskListId.collectAsStateWithLifecycle(null)

    // need to store a saveable (Serializable/Parcelable) object
    // rememberListDetailPaneScaffoldNavigator, under the hood uses rememberSaveable with it
    // we use the TaskListUIModel.id as the key to save the state of the navigator
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var errorEvent by remember { mutableStateOf<TaskEvent.Error?>(null) }
    var networkBannerDismissed by remember { mutableStateOf(true) }
    val isNetworkAvailable by networkStateFlow().collectAsStateWithLifecycle(null)

    LaunchedEffect(selectedTaskListId) {
        if (selectedTaskListId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedTaskListId?.value)
        }
    }

    MyBackHandler(navigator::canNavigateBack) {
        scope.launch {
            // FIXME still a bit fragile depending on contentKey use depending on destination
            //  it might be 2 destinations using the same value as contentKey
            //  will be good enough to begin with
            if (navigator.currentDestination?.contentKey == selectedTaskListId?.value) {
                viewModel.selectTaskList(null)
            }
            navigator.navigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            // Only display error for now
            if (event is TaskEvent.Error) {
                errorEvent = event
            }
        }
    }

    errorEvent?.let { event ->
        val errorString = stringResource(event.asLabel)
        LaunchedEffect(errorString) {
            snackbarHostState.showSnackbar(errorString)
            errorEvent = null
        }
    }

    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable == false) {
            networkBannerDismissed = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            AnimatedVisibility(!networkBannerDismissed && isNetworkAvailable == false) {
                Banner(
                    message = stringResource(Res.string.network_unavailable_message),
                    icon = LucideIcons.CloudOff,
                ) {
                    networkBannerDismissed = true
                }
            }
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {

                    val lists = taskLists
                    AnimatedPane {
                        when {
                            lists == null -> LoadingPane()

                            lists.isEmpty() -> {
                                val newTaskListName = stringResource(Res.string.task_lists_screen_default_task_list_title)
                                NoTaskListsEmptyState {
                                    onNewTaskList(newTaskListName)
                                }
                            }

                            else -> Row {
                                TaskListsColumn(
                                    lists,
                                    onNewTaskList = { onNewTaskList("") },
                                    onItemClick = { taskList ->
                                        viewModel.selectTaskList(taskList.id)
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
                    TaskListDetail(viewModel)
                },
            )
        }
    }
}