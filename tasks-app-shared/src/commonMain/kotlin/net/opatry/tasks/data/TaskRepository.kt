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

package net.opatry.tasks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.model.TaskListDataModel

private fun TaskList.asTaskListEntity(localId: Long?): TaskListEntity {
    return TaskListEntity(
        id = localId ?: 0,
        remoteId = id,
        title = title
    )
}

class TaskRepository(
    private val taskListDao: TaskListDao,
    private val taskListsApi: TaskListsApi,
    private val tasksApi: TasksApi,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTaskLists() = taskListDao.getAllAsFlow().mapLatest { taskListEntities ->
        taskListEntities.map { TaskListDataModel(it.id, it.title, Clock.System.now(), emptyList()) }
    }

    suspend fun fetchTaskLists() {
        withContext(Dispatchers.IO) {
            taskListsApi.listAll()
        }.forEach {
            // FIXME suboptimal
            //  - check stale ones in DB and remove them if not only local
            //  - check no update, and ignore/filter
            //  - check new ones
            //  - etc.
            val localId = taskListDao.getByRemoteId(it.id)?.id ?: 0
            taskListDao.insert(it.asTaskListEntity(localId))
        }
    }

    suspend fun createTaskList(title: String): TaskList {
        taskListDao.insert(TaskListEntity(title = title))
        return withContext(Dispatchers.IO) {
            taskListsApi.insert(TaskList(title = title))
        }
    }

    suspend fun createTask(taskListId: Long, title: String, dueDate: Instant? = null) {
//        withContext(Dispatchers.IO) {
//            tasksApi.insert(
//                taskList.id,
//                Task(
//                    title = title,
//                    dueDate = dueDate
//                )
//            )
//        }
    }
}
