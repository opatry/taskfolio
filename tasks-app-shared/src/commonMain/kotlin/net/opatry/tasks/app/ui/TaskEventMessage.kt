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

import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_event_error_task_create
import net.opatry.tasks.resources.task_event_error_task_create_child
import net.opatry.tasks.resources.task_event_error_task_delete
import net.opatry.tasks.resources.task_event_error_task_indent
import net.opatry.tasks.resources.task_event_error_task_move
import net.opatry.tasks.resources.task_event_error_task_restore
import net.opatry.tasks.resources.task_event_error_task_unindent
import net.opatry.tasks.resources.task_event_error_task_update
import net.opatry.tasks.resources.task_event_error_tasklist_clear_completed
import net.opatry.tasks.resources.task_event_error_tasklist_create
import net.opatry.tasks.resources.task_event_error_tasklist_delete
import net.opatry.tasks.resources.task_event_error_tasklist_rename
import net.opatry.tasks.resources.task_event_error_tasklist_sort
import org.jetbrains.compose.resources.StringResource


val TaskEvent.Error.asLabel: StringResource
    get() = when (this) {
        TaskEvent.Error.Task.Create -> Res.string.task_event_error_task_create
        TaskEvent.Error.Task.CreateChild -> Res.string.task_event_error_task_create_child
        TaskEvent.Error.Task.Delete -> Res.string.task_event_error_task_delete
        TaskEvent.Error.Task.Unindent -> Res.string.task_event_error_task_unindent
        TaskEvent.Error.Task.Indent -> Res.string.task_event_error_task_indent
        TaskEvent.Error.Task.Move -> Res.string.task_event_error_task_move
        TaskEvent.Error.Task.Restore -> Res.string.task_event_error_task_restore
        TaskEvent.Error.Task.ToggleCompletionState,
        TaskEvent.Error.Task.Update -> Res.string.task_event_error_task_update

        TaskEvent.Error.TaskList.Create -> Res.string.task_event_error_tasklist_create
        TaskEvent.Error.TaskList.Delete -> Res.string.task_event_error_tasklist_delete
        TaskEvent.Error.TaskList.Rename -> Res.string.task_event_error_tasklist_rename
        TaskEvent.Error.TaskList.ClearCompletedTasks -> Res.string.task_event_error_tasklist_clear_completed
        TaskEvent.Error.TaskList.Sort -> Res.string.task_event_error_tasklist_sort
    }