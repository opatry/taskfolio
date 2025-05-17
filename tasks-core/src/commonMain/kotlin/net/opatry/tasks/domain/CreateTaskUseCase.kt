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

package net.opatry.tasks.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import net.opatry.google.tasks.TasksApi
import net.opatry.tasks.NowProvider
import net.opatry.tasks.data.TaskDao
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.asTask
import net.opatry.tasks.data.asTaskEntity
import net.opatry.tasks.data.computeTaskPositions
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.toTaskPosition

class CreateTaskUseCase(
    // TODO use repository to deal with DB & Remote
    //  quick & dirty to see UseCase extraction result/benefit
    //  private val repository: TaskRepository,
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao,
    private val tasksApi: TasksApi,
    private val clockNow: NowProvider = Clock.System::now,
) {
    suspend operator fun invoke(
        taskListId: TaskListId,
        parentTaskId: TaskId?,
        title: String,
        notes: String = "",
        dueDate: LocalDate? = null
    ): TaskId {
        return delegate(taskListId.value, parentTaskId?.value, title, notes, dueDate).let(::TaskId)
    }

    private suspend fun delegate(taskListId: Long, parentTaskId: Long?, title: String, notes: String, dueDate: LocalDate?): Long {
        val taskListEntity = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
        val parentTaskEntity = parentTaskId?.let { requireNotNull(taskDao.getById(it)) { "Invalid parent task id $parentTaskId" } }
        val now = clockNow()
        val firstPosition = 0.toTaskPosition()
        val currentTasks = taskDao.getTasksFromPositionOnward(taskListId, parentTaskId, firstPosition)
            .toMutableList()
        val taskEntity = TaskEntity(
            parentListLocalId = taskListId,
            parentTaskLocalId = parentTaskId,
            title = title,
            notes = notes,
            lastUpdateDate = now,
            dueDate = dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault()),
            position = firstPosition,
        )
        currentTasks.add(0, taskEntity)
        val updatedTasks = computeTaskPositions(currentTasks)
        val taskId = taskDao.upsertAll(updatedTasks).first()

        // FIXME should already be available in entity, quick & dirty workaround
        val parentTaskRemoteId = parentTaskEntity?.remoteId
            ?: parentTaskId?.let { taskListDao.getById(it) }?.remoteId
        if (taskListEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.insert(taskListEntity.remoteId, taskEntity.asTask(), parentTaskRemoteId)
                } catch (_: Exception) {
                    null
                }
            }
            if (task != null) {
                taskDao.upsert(task.asTaskEntity(taskListId, taskId, parentTaskId))
            }
        }
        return taskId
    }
}