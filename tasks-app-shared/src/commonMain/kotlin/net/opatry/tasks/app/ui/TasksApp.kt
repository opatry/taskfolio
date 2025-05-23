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

package net.opatry.tasks.app.ui

import Calendar
import Info
import ListTodo
import LucideIcons
import Search
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.presentation.UserState
import net.opatry.tasks.app.presentation.UserViewModel
import net.opatry.tasks.app.ui.component.MissingScreen
import net.opatry.tasks.app.ui.component.NewTaskListDialog
import net.opatry.tasks.app.ui.component.ProfileIcon
import net.opatry.tasks.app.ui.screen.AboutApp
import net.opatry.tasks.app.ui.screen.AboutScreen
import net.opatry.tasks.app.ui.screen.TaskListsMasterDetail
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.app_name
import net.opatry.tasks.resources.navigation_about
import net.opatry.tasks.resources.navigation_calendar
import net.opatry.tasks.resources.navigation_search
import net.opatry.tasks.resources.navigation_tasks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class AppTasksScreen(
    val labelRes: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource? = null,
) {
    Tasks(Res.string.navigation_tasks, LucideIcons.ListTodo),
    Calendar(Res.string.navigation_calendar, LucideIcons.Calendar),
    Search(Res.string.navigation_search, LucideIcons.Search),
    About(Res.string.navigation_about, LucideIcons.Info),
}

@Composable
fun TasksApp(aboutApp: AboutApp, userViewModel: UserViewModel, tasksViewModel: TaskListsViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val userState by userViewModel.state.collectAsStateWithLifecycle(null)
    val isSigned by remember(userState) {
        derivedStateOf {
            userState is UserState.SignedIn
        }
    }

    val isFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(isFocused, isSigned) {
        tasksViewModel.enableAutoRefresh(isFocused && isSigned)
    }

    var newTaskListDefaultTitle by remember { mutableStateOf("") }
    var showNewTaskListDialog by remember { mutableStateOf(false) }

    NavigationSuiteScaffold(navigationSuiteItems = {
        AppTasksScreen.entries.forEach { screen ->
            // hide unsupported screens for now
            if (screen == AppTasksScreen.Calendar) return@forEach
            if (screen == AppTasksScreen.Search) return@forEach
            item(
                selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.name } == true,
                onClick = { navController.navigateWithBackStackHandling(screen.name) },
                label = { Text(stringResource(screen.labelRes)) },
                icon = {
                    Icon(screen.icon, screen.contentDescription?.let { stringResource(it) })
                },
                alwaysShowLabel = false,
            )
        }
    }) {
        NavHost(navController, AppTasksScreen.Tasks.name) {
            composable(AppTasksScreen.Tasks.name) {
                Column {
                    Card(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(Res.string.app_name),
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            ProfileIcon(userViewModel)
                        }
                    }

                    TaskListsMasterDetail(tasksViewModel) { title ->
                        newTaskListDefaultTitle = title
                        showNewTaskListDialog = true
                    }

                    if (showNewTaskListDialog) {
                        NewTaskListDialog(
                            defaultTitle = newTaskListDefaultTitle,
                            onDismissRequest = { showNewTaskListDialog = false },
                            onCreate = { title ->
                                showNewTaskListDialog = false
                                tasksViewModel.createTaskList(title)
                            },
                        )
                    }
                }
            }

            composable(AppTasksScreen.Calendar.name) {
                MissingScreen(stringResource(AppTasksScreen.Calendar.labelRes), LucideIcons.Calendar)
            }

            composable(AppTasksScreen.Search.name) {
                MissingScreen(stringResource(AppTasksScreen.Search.labelRes), LucideIcons.Search)
            }

            composable(AppTasksScreen.About.name) {
                AboutScreen(aboutApp)
            }
        }
    }
}

fun NavHostController.navigateWithBackStackHandling(route: String) {
    // avoids stacking A/B/A/B/A/B when navigating back and forth between A and B using bottom sheet
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}