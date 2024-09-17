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

package net.opatry.tasks.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import net.opatry.google.tasks.model.TaskList
import ListTodo as LucideListTodo
import Search as LucideSearch
import Settings as LucideSettings

enum class Destination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String? = null,
) {
    Tasks("Account", LucideListTodo),
    Search("Search", LucideSearch),
    Settings("Settings", LucideSettings),
}

@Composable
fun TasksApp(taskLists: List<TaskList>) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Tasks")
            })
        },
    ) { innerPadding ->
        // TODO scaffold + bottom bar vs nav rail WindowSizeClass
        // TODO where to put Theme+Surface

        val selectedItem = remember { mutableStateOf(Destination.Tasks) }
        NavigationSuiteScaffold(navigationSuiteItems = {
            Destination.entries.forEach {
                item(
                    selected = selectedItem.value == it,
                    onClick = { selectedItem.value = it },
                    label = { Text(it.label) },
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.contentDescription
                        )
                    },
                    alwaysShowLabel = true,
                )
            }
        }) {
            Box(Modifier.fillMaxSize()) {
                Surface(Modifier.align(Alignment.Center)) {
                    when (selectedItem.value) {
                        Destination.Tasks -> TasksScreen(taskLists)
                        Destination.Search -> SearchScreen()
                        Destination.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun TasksScreen(taskLists: List<TaskList>) {
    if (taskLists.isEmpty()) {
        Text("No task list")
    } else {
        LazyColumn {
            item(taskLists) {
                taskLists.forEach {
                    Text(it.title)
                }
            }
        }
    }
}

@Composable
fun SearchScreen() {
    Text("Search")
}

@Composable
fun SettingsScreen() {
    Text("Settings")
}
