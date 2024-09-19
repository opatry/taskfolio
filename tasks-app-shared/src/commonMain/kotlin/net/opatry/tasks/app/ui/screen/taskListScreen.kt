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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opatry.google.tasks.model.Task
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.component.TaskListColumn
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_empty_state_title
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random


@Composable
fun TaskListsScreen(viewModel: TaskListsViewModel) {
    val taskLists by viewModel.taskLists.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        if (taskLists.isEmpty()) {
            Text(stringResource(Res.string.task_lists_screen_empty_state_title))
        } else {
            // TODO master detail layout

            LazyRow(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TODO stub + UI data model
                val tasks = List(30) { Task(title = "This is my task $it") }
                items(taskLists) { taskList ->
                    // TODO provide width
                    TaskListColumn(taskList, tasks.take(Random.nextInt(tasks.size))) {
                        // TODO dialog to ask for data
                        viewModel.createTask(taskList, "This is a new task")
                    }
                }
            }
        }
    }
}
