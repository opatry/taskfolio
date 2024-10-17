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

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel


val today: LocalDate
    get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

private var TASK_LIST_ID = 0L
fun createTaskList(
    remainingTaskCount: Int = 0,
    completedTaskCount: Int = 0,
) = TaskListUIModel(
    id = TASK_LIST_ID++,
    title = "Task List",
    lastUpdate = LocalDate.Format { year(); char('-'); monthNumber(); char('-'); dayOfMonth() }.format(today),
    remainingTasks = mapOf(null to List(remainingTaskCount) { createTask() }),
    completedTasks = List(completedTaskCount) { createTask(isCompleted = true) }
)

private var TASK_ID = 0L
fun createTask(
    title: String = "Task",
    dueDate: LocalDate = today,
    isCompleted: Boolean = false,
) = TaskUIModel(
    id = TASK_ID++,
    title = title,
    dueDate = dueDate,
    isCompleted = isCompleted,
)
