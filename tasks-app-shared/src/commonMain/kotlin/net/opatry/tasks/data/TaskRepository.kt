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
import kotlinx.coroutines.flow.distinctUntilChanged
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
        etag = etag,
        title = title,
    )
}

private fun Task.asTaskEntity(parentLocalId: Long, localId: Long?): TaskEntity {
    return TaskEntity(
        id = localId ?: 0,
        remoteId = id,
        parentListLocalId = parentLocalId,
        etag = etag,
        title = title,
        notes = notes ?: "",
    )
}

private fun TaskListEntity.asTaskListDataModel(tasks: List<TaskEntity>): TaskListDataModel {
    return TaskListDataModel(
        id = id,
        title = title,
        lastUpdate = Clock.System.now(),
        tasks = tasks.map(TaskEntity::asTaskDataModel)
    )
}

private fun TaskEntity.asTaskDataModel(): TaskDataModel {
    return TaskDataModel(
        id = id,
        title = title,
        notes = notes,
        dueDate = Clock.System.now()
    )
}

class TaskRepository(
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao,
    private val taskListsApi: TaskListsApi,
    private val tasksApi: TasksApi,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTaskLists() = taskListDao.getAllTaskListsWithTasksAsFlow()
        .distinctUntilChanged()
        .mapLatest { entries ->
            entries.map { (list, tasks) ->
            list.asTaskListDataModel(tasks)
        }
    }

    suspend fun sync() {
        val taskListIds = mutableMapOf<Long, String>()
        val remoteTaskLists = withContext(Dispatchers.IO) {
            try {
                taskListsApi.listAll()
            } catch (e: Exception) {
                emptyList()
            }
        }
        remoteTaskLists.onEach { remoteTaskList ->
            // FIXME suboptimal
            //  - check stale ones in DB and remove them if not only local
            //  - check no update, and ignore/filter
            //  - check new ones
            //  - etc.
            val existingEntity = taskListDao.getByRemoteId(remoteTaskList.id)
            val finalLocalId = taskListDao.insert(remoteTaskList.asTaskListEntity(existingEntity?.id))
            println("task list ${remoteTaskList.etag} vs ${existingEntity?.etag}) with final local id $finalLocalId")
            taskListIds[finalLocalId] = remoteTaskList.id
        }
        taskListDao.deleteStaleTaskLists(remoteTaskLists.map(TaskList::id))
        taskListDao.getLocalOnlyTaskLists().onEach { localTaskList ->
            val remoteId = try {
                taskListsApi.insert(TaskList(title = localTaskList.title)).id
            } catch (e: Exception) {
                null
            }
            if (remoteId != null) {
                taskListDao.insert(localTaskList.copy(remoteId = remoteId))
            }
        }
        taskListIds.forEach { (localListId, remoteListId) ->
            // TODO deal with showDeleted, showHidden, etc.
            // TODO updatedMin could be used to filter out unchanged tasks since last sync
            //  /!\ this would impact the deleteStaleTasks logic
            val remoteTasks = tasksApi.listAll(remoteListId)
            remoteTasks.onEach { remoteTask ->
                val existingEntity = taskDao.getByRemoteId(remoteTask.id)
                taskDao.insert(remoteTask.asTaskEntity(localListId, existingEntity?.id))
            }
            taskDao.deleteStaleTasks(localListId, remoteTasks.map(Task::id))
            taskDao.getLocalOnlyTasks().onEach { localTask ->
                val remoteId = try {
                    tasksApi.insert(remoteListId, Task(title = localTask.title)).id
                } catch (e: Exception) {
                    null
                }
                if (remoteId != null) {
                    taskDao.insert(localTask.copy(remoteId = remoteId))
                }
            }
        }
    }

    suspend fun createTaskList(title: String) {
        val taskListId = taskListDao.insert(TaskListEntity(title = title))
        val taskListEntity = taskListDao.getById(taskListId)
        if (taskListEntity != null) {
            val taskList = withContext(Dispatchers.IO) {
                try {
                    taskListsApi.insert(TaskList(title = title))
                } catch (e: Exception) {
                    null
                }
            }
            if (taskList != null) {
                taskListDao.insert(taskList.asTaskListEntity(taskListId))
            }
        }
    }

    suspend fun createTask(taskListId: Long, title: String, dueDate: Instant? = null) {
        val taskId = taskDao.insert(TaskEntity(parentListLocalId = taskListId, title = title))
        val taskListEntity = taskListDao.getById(taskListId)
        if (taskListEntity?.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.insert(
                        taskListEntity.remoteId,
                        Task(
                            title = title,
                            dueDate = dueDate
                        )
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (task != null) {
                taskDao.insert(task.asTaskEntity(taskListId, taskId))
            }
        }
    }
}
