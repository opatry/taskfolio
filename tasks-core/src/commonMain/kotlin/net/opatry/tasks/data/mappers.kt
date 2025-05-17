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

package net.opatry.tasks.data

import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.tasks.domain.TaskListSorting

internal fun TaskList.asTaskListEntity(localId: Long?, sorting: TaskListEntity.Sorting): TaskListEntity {
    return TaskListEntity(
        id = localId ?: 0,
        remoteId = id,
        etag = etag,
        title = title,
        lastUpdateDate = updatedDate,
        sorting = sorting,
    )
}

internal fun Task.asTaskEntity(parentLocalId: Long, localId: Long?, parentTaskLocalId: Long?): TaskEntity {
    return TaskEntity(
        id = localId ?: 0,
        remoteId = id,
        parentListLocalId = parentLocalId,
        parentTaskLocalId = parentTaskLocalId,
        parentTaskRemoteId = parent,
        etag = etag,
        title = title,
        notes = notes ?: "",
        dueDate = dueDate,
        lastUpdateDate = updatedDate,
        completionDate = completedDate,
        isCompleted = isCompleted,
        position = position,
    )
}

internal fun TaskListEntity.asTaskListDataModel(tasks: List<TaskEntity>): TaskListDataModel {
    val (sorting, sortedTasks) = when (sorting) {
        TaskListEntity.Sorting.UserDefined -> TaskListSorting.Manual to sortTasksManualOrdering(tasks).map { (task, indent) ->
            task.asTaskDataModel(indent)
        }

        TaskListEntity.Sorting.DueDate -> TaskListSorting.DueDate to sortTasksDateOrdering(tasks).map { task ->
            task.asTaskDataModel(0)
        }

        TaskListEntity.Sorting.Title -> TaskListSorting.Title to sortTasksTitleOrdering(tasks).map { task ->
            task.asTaskDataModel(0)
        }
    }
    return TaskListDataModel(
        id = id,
        title = title,
        lastUpdate = lastUpdateDate,
        tasks = sortedTasks,
        sorting = sorting
    )
}

internal fun TaskEntity.asTaskDataModel(indent: Int): TaskDataModel {
    return TaskDataModel(
        id = id,
        title = title,
        notes = notes,
        isCompleted = isCompleted,
        dueDate = dueDate,
        lastUpdateDate = lastUpdateDate,
        completionDate = completionDate,
        position = position,
        indent = indent,
    )
}

internal fun TaskEntity.asTask(): Task {
    return Task(
        id = remoteId ?: "",
        title = title,
        notes = notes,
        dueDate = dueDate,
        updatedDate = lastUpdateDate,
        status = if (isCompleted) Task.Status.Completed else Task.Status.NeedsAction,
        completedDate = completionDate,
        // doc says it's a read only field, but status is not hidden when syncing local only completed tasks
        // forcing the hidden status works and makes everything more consistent (position following 099999... pattern, hidden status)
        isHidden = isCompleted,
        position = position,
    )
}