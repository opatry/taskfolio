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
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel

private fun TaskList.asTaskListEntity(localId: Long?): TaskListEntity {
    return TaskListEntity(
        id = localId ?: 0,
        remoteId = id,
        title = title
    )
}
private fun Task.asTaskEntity(parentLocalId: Long, localId: Long?): TaskEntity {
    return TaskEntity(
        id = localId ?: 0,
        remoteId = id,
        parentListLocalId = parentLocalId,
        title = title,
        notes = notes ?: "",
    )
}

class TaskRepository(
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao,
    private val taskListsApi: TaskListsApi,
    private val tasksApi: TasksApi,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTaskLists() = taskListDao.getAllAsFlow().mapLatest { taskListEntities ->
        // FIXME do it in a single query
        // TODO mapper fn
        taskListEntities.map { listEntity ->
            val children = taskDao.getAllByParentId(listEntity.id).map {
                TaskDataModel(it.id, it.title, it.notes, Clock.System.now())
            }
            TaskListDataModel(listEntity.id, listEntity.title, Clock.System.now(), children)
        }
    }

    suspend fun fetchTaskLists() {
        val taskListIds = mutableMapOf<Long, String>()
        withContext(Dispatchers.IO) {
            taskListsApi.listAll()
        }.onEach {
            // FIXME suboptimal
            //  - check stale ones in DB and remove them if not only local
            //  - check no update, and ignore/filter
            //  - check new ones
            //  - etc.
            val localId = taskListDao.getByRemoteId(it.id)?.id ?: 0
            val finalLocalId = taskListDao.insert(it.asTaskListEntity(localId))
            taskListIds[finalLocalId] = it.id
        }
        taskListIds.forEach { (localListId, remoteListId) ->
            tasksApi.listAll(remoteListId).onEach { task ->
                val localTaskId = taskDao.getByRemoteId(task.id)?.id ?: 0
                taskDao.insert(task.asTaskEntity(localListId, localTaskId))
            }
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
