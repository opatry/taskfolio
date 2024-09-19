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

import CircleFadingPlus
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_add_task
import net.opatry.tasks.resources.task_lists_screen_empty_list_desc
import net.opatry.tasks.resources.task_lists_screen_empty_list_title
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToSvgPainter
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class, ExperimentalResourceApi::class)
@Composable
fun TaskListColumn(taskList: TaskList, tasks: List<Task>, onNewTaskClick: () -> Unit) {
    Card(Modifier.width(250.dp)) {
        LazyColumn {
            stickyHeader {
                // TODO elevation on lift
                Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                    Text(
                        taskList.title,
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onNewTaskClick) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(CircleFadingPlus, null)
                            Text(stringResource(Res.string.task_lists_screen_add_task))
                        }
                    }
                }
            }
            if (tasks.isEmpty()) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {

                        // FIXME async load + caching, feasible with Coil & KMP Res?
                        val bytes = runBlocking {
                            Res.readBytes("files/undraw_to_do_list_re_9nt7.svg")
                        }
                        Image(bytes.decodeToSvgPainter(LocalDensity.current), null)

//                        AsyncImage(Res.getUri("files/undraw_to_do_list_re_9nt7.svg"), null)
                        Text(stringResource(Res.string.task_lists_screen_empty_list_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(Res.string.task_lists_screen_empty_list_desc), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(tasks) { task ->
                    // TODO indentation & parent hierarchy
                    TaskRow(task)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TaskRow(task: Task) {
    // TODO nice rounded checks or so?
    ListItem(
        icon = {
            Checkbox(checked = task.isCompleted, onCheckedChange = {})
        }
    ) {
        Text(task.title)
    }
}
