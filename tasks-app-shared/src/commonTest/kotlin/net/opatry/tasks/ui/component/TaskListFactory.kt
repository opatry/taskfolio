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

package net.opatry.tasks.ui.component

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel


val today: LocalDate
    get() = Clock.System.todayIn(TimeZone.UTC)

private var TASK_LIST_ID = 0L
fun createTaskList(
    title: String = "Task List",
    remainingTaskCount: Int = 0,
    completedTaskCount: Int = 0,
) = TaskListUIModel(
    id = TaskListId(TASK_LIST_ID++),
    title = title,
    remainingTasks = mapOf(null to List(remainingTaskCount) { createTask() }),
    completedTasks = List(completedTaskCount) { createCompletedTask() }
)

private var TASK_ID = 0L
fun createTask(
    title: String = "Task",
    dueDate: LocalDate = today,
    canMoveToTop: Boolean = false,
    canUnindent: Boolean = false,
    canIndent: Boolean = false,
    canCreateSubTask: Boolean = false,
) = TaskUIModel.Todo(
    id = TaskId(TASK_ID++),
    title = title,
    dueDate = dueDate,
    canMoveToTop = canMoveToTop,
    canUnindent = canUnindent,
    canIndent = canIndent,
    canCreateSubTask = canCreateSubTask,
)

fun createCompletedTask(
    title: String = "Task",
    dueDate: LocalDate = today,
    completionDate: LocalDate = today,
) = TaskUIModel.Done(
    id = TaskId(TASK_ID++),
    title = title,
    dueDate = dueDate,
    completionDate = completionDate,
)
