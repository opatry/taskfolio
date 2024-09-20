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
        position = position,
        // TODO parentTaskLocalId = ,
        parentTaskRemoteId = parent,
    )
}

private fun TaskListEntity.asTaskListDataModel(tasks: List<TaskEntity>): TaskListDataModel {
    val sortedTasks = sortTasks(tasks).map { (task, indent) ->
        task.asTaskDataModel(indent)
    }
    return TaskListDataModel(
        id = id,
        title = title,
        lastUpdate = Clock.System.now(),
        tasks = sortedTasks,
    )
}

private fun TaskEntity.asTaskDataModel(indent: Int): TaskDataModel {
    return TaskDataModel(
        id = id,
        title = title,
        notes = notes,
        dueDate = Clock.System.now(),
        position = position,
        indent = indent,
    )
}

fun sortTasks(tasks: List<TaskEntity>): List<Pair<TaskEntity, Int>> {
    // Step 1: Create a map of tasks by their IDs for easy lookup
    val taskMap = tasks.associateBy { it.remoteId }.toMutableMap() // FIXME local data only?

    // Step 2: Build a tree structure with parent-child relationships
    val tree = mutableMapOf<String, MutableList<TaskEntity>>()
    tasks.forEach { task ->
        val parentId = task.parentTaskRemoteId // FIXME local data only?
        if (parentId == null) {
            // Tasks with no parent go directly into the root of the tree
            tree.getOrPut(task.remoteId ?: "") { mutableListOf() } // FIXME local data only?
        } else {
            // Add child task under its parent's list of children
            tree.getOrPut(parentId) { mutableListOf() }.add(task)
        }
    }

    // Step 3: Sort the child tasks by position
    tree.forEach { (_, children) ->
        children.sortBy { it.position }
    }

    // Step 4: Recursive function to traverse tasks and assign indentation levels
    fun traverseTasks(taskId: String, level: Int, result: MutableList<Pair<TaskEntity, Int>>) {
        val task = taskMap[taskId] ?: return
        result.add(task to level)
        val children = tree[taskId] ?: return
        for (child in children) {
            traverseTasks(child.remoteId ?: "", level + 1, result) // FIXME local data only?
        }
    }

    // Step 5: Start traversal from the root tasks (tasks with no parents)
    val sortedTasks = mutableListOf<Pair<TaskEntity, Int>>()
    tree.keys.filter { taskMap[it]?.parentTaskRemoteId == null }.sortedBy { taskMap[it]?.position }.forEach {
        traverseTasks(it, 0, sortedTasks)
    }

    return sortedTasks
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
                // FIXME Where should happen the sorting, on SQL side or here or in UI layer?
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
        val taskId = taskDao.insert(TaskEntity(parentListLocalId = taskListId, title = title, position = ""/*TODO local position value?*/))
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
