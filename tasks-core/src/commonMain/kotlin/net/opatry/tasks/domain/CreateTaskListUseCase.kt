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
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.NowProvider
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.asTaskListEntity
import net.opatry.tasks.data.entity.TaskListEntity

class CreateTaskListUseCase(
    // TODO use repository to deal with DB & Remote
    //  quick & dirty to see UseCase extraction result/benefit
    //  private val repository: TaskRepository,
    private val taskListDao: TaskListDao,
    private val taskListsApi: TaskListsApi,
    private val clockNow: NowProvider = Clock.System::now,
) {
    suspend operator fun invoke(title: String): TaskListId {
        val now = clockNow()
        val taskListId = taskListDao.insert(TaskListEntity(title = title, lastUpdateDate = now))
        val taskList = withContext(Dispatchers.IO) {
            try {
                taskListsApi.insert(TaskList(title = title, updatedDate = now))
            } catch (_: Exception) {
                null
            }
        }
        if (taskList != null) {
            taskListDao.upsert(taskList.asTaskListEntity(taskListId, TaskListEntity.Sorting.UserDefined))
        }
        return TaskListId(taskListId)
    }
}