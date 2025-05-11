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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.listAll
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import java.math.BigInteger

enum class TaskListSorting {
    Manual,
    DueDate,
}

private fun TaskList.asTaskListEntity(localId: Long?, sorting: TaskListEntity.Sorting): TaskListEntity {
    return TaskListEntity(
        id = localId ?: 0,
        remoteId = id,
        etag = etag,
        title = title,
        lastUpdateDate = updatedDate,
        sorting = sorting,
    )
}

private fun Task.asTaskEntity(parentLocalId: Long, localId: Long?): TaskEntity {
    return TaskEntity(
        id = localId ?: 0,
        remoteId = id,
        parentListLocalId = parentLocalId,
        // TODO parentTaskLocalId = ,
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

private fun TaskListEntity.asTaskListDataModel(tasks: List<TaskEntity>): TaskListDataModel {
    val (sorting, sortedTasks) = when (sorting) {
        TaskListEntity.Sorting.UserDefined -> TaskListSorting.Manual to sortTasksManualOrdering(tasks).map { (task, indent) ->
            task.asTaskDataModel(indent)
        }

        TaskListEntity.Sorting.DueDate -> TaskListSorting.DueDate to sortTasksDateOrdering(tasks).map { task ->
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

private fun TaskEntity.asTaskDataModel(indent: Int): TaskDataModel {
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

private fun TaskEntity.asTask(): Task {
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

fun sortTasksManualOrdering(tasks: List<TaskEntity>): List<Pair<TaskEntity, Int>> {
    // Step 1: Create a map of tasks by their IDs for easy lookup
    val taskMap = tasks.associateBy { it.remoteId ?: it.id.toString() }.toMutableMap()

    // Step 2: Build a tree structure with parent-child relationships
    val tree = mutableMapOf<String, MutableList<TaskEntity>>()
    tasks.forEach { task ->
        val parentId = task.parentTaskRemoteId ?: task.parentTaskLocalId?.toString()
        if (parentId == null) {
            // Tasks with no parent go directly into the root of the tree
            tree.getOrPut(task.remoteId ?: task.id.toString()) { mutableListOf() }
        } else {
            // Add child task under its parent's list of children
            tree.getOrPut(parentId) { mutableListOf() }.add(task)
        }
    }

    // Step 3: Sort the child tasks by position
    tree.forEach { (_, children) ->
        children.sortBy(TaskEntity::position)
    }

    // Step 4: Recursive function to traverse tasks and assign indentation levels
    fun traverseTasks(taskId: String, level: Int, result: MutableList<Pair<TaskEntity, Int>>) {
        val task = taskMap[taskId] ?: return
        result.add(task to level)
        val children = tree[taskId] ?: return
        for (child in children) {
            traverseTasks(child.remoteId ?: child.id.toString(), level + 1, result)
        }
    }

    // Step 5: Start traversal from the root tasks (tasks with no parents)
    val sortedTasks = mutableListOf<Pair<TaskEntity, Int>>()
    tree.keys.filter { taskMap[it]?.parentTaskRemoteId == null }.sortedBy { taskMap[it]?.position }.forEach {
        traverseTasks(it, 0, sortedTasks)
    }

    return sortedTasks
}

fun sortTasksDateOrdering(tasks: List<TaskEntity>): List<TaskEntity> {
    return tasks.sortedBy(TaskEntity::dueDate)
}

/**
 * Updates the position of tasks among the provided list of tasks.
 * The tasks to complete are kept in the same order and their position is recomputed starting from 0.
 * The completed tasks are sorted last and sorted by completion date.
 * Position is reset for each list & parent task.
 */
fun computeTaskPositions(tasks: List<TaskEntity>): List<TaskEntity> {
    return buildList {
        val tasksByList = tasks.groupBy(TaskEntity::parentListLocalId)
        tasksByList.forEach { (_, tasks) ->
            tasks.groupBy(TaskEntity::parentTaskLocalId).forEach { (_, subTasks) ->
                val (completed, todo) = subTasks.partition { it.isCompleted && it.completionDate != null }
                val completedWithPositions = completed.map { it.copy(position = computeCompletedTaskPosition(it)) }
                val todoWithPositions = todo.mapIndexed { index, taskEntity ->
                    taskEntity.copy(position = index.toTaskPosition())
                }

                val sortedSubTasks = (todoWithPositions + completedWithPositions).sortedBy(TaskEntity::position)
                addAll(sortedSubTasks)
            }
        }
    }
}

fun Number.toTaskPosition(): String = this.toString().padStart(20, '0')

fun computeCompletedTaskPosition(task: TaskEntity): String {
    val completionDate = task.completionDate
    require(task.isCompleted && completionDate != null) {
        "Task must be completed and have a completion date"
    }
    // ignore milliseconds, on backend side, Google Tasks API truncates milliseconds
    val truncatedDate = Instant.fromEpochMilliseconds(completionDate.toEpochMilliseconds() / 1000 * 1000)
    return truncatedDate.asCompletedTaskPosition()
}

/**
 * Converts a completion date as the position of a completed task for Google Tasks sorting logic.
 * The sorting of completed tasks puts last completed tasks first.
 */
fun Instant.asCompletedTaskPosition(): String {
    val upperBound = BigInteger("9999999999999999999")
    val sorting = upperBound - this.toEpochMilliseconds().toBigInteger()
    return sorting.toTaskPosition()
}

class TaskRepository(
    private val taskListDao: TaskListDao,
    private val taskDao: TaskDao,
    private val taskListsApi: TaskListsApi,
    private val tasksApi: TasksApi,
    private val clockNow: () -> Instant = Clock.System::now,
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
            } catch (_: Exception) {
                null
            }
        }

        if (remoteTaskLists == null) {
            // most likely not internet, can't fetch data, nothing to sync
            return
        }

        remoteTaskLists.onEach { remoteTaskList ->
            // FIXME suboptimal
            //  - check stale ones in DB and remove them if not only local
            //  - check no update, and ignore/filter
            //  - check new ones
            //  - etc.
            val existingEntity = taskListDao.getByRemoteId(remoteTaskList.id)
            val updatedEntity = remoteTaskList.asTaskListEntity(existingEntity?.id, existingEntity?.sorting ?: TaskListEntity.Sorting.UserDefined)
            val finalLocalId = taskListDao.upsert(updatedEntity)
            taskListIds[finalLocalId] = remoteTaskList.id
        }
        taskListDao.deleteStaleTaskLists(remoteTaskLists.map(TaskList::id))
        taskListDao.getLocalOnlyTaskLists().onEach { localTaskList ->
            val remoteTaskList = withContext(Dispatchers.IO) {
                try {
                    taskListsApi.insert(TaskList(localTaskList.title))
                } catch (_: Exception) {
                    null
                }
            }
            if (remoteTaskList != null) {
                taskListDao.upsert(remoteTaskList.asTaskListEntity(localTaskList.id, localTaskList.sorting))
            }
        }
        taskListIds.forEach { (localListId, remoteListId) ->
            // TODO deal with showDeleted, showHidden, etc.
            // TODO updatedMin could be used to filter out unchanged tasks since last sync
            //  /!\ this would impact the deleteStaleTasks logic
            val remoteTasks = withContext(Dispatchers.IO) {
                tasksApi.listAll(remoteListId, showHidden = true, showCompleted = true)
            }
            remoteTasks.onEach { remoteTask ->
                val existingEntity = taskDao.getByRemoteId(remoteTask.id)
                taskDao.upsert(remoteTask.asTaskEntity(localListId, existingEntity?.id))
            }
            taskDao.deleteStaleTasks(localListId, remoteTasks.map(Task::id))
            taskDao.getLocalOnlyTasks(localListId).onEach { localTask ->
                val remoteTask = withContext(Dispatchers.IO) {
                    try {
                        tasksApi.insert(remoteListId, localTask.asTask())
                    } catch (_: Exception) {
                        null
                    }
                }
                if (remoteTask != null) {
                    taskDao.upsert(remoteTask.asTaskEntity(localListId, localTask.id))
                }
            }
        }
    }

    suspend fun createTaskList(title: String) {
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
    }

    suspend fun deleteTaskList(taskListId: Long) {
        // TODO deal with deleted locally but not remotely yet (no internet)
        val taskListEntity = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
        taskListDao.deleteTaskList(taskListId)
        if (taskListEntity.remoteId != null) {
            withContext(Dispatchers.IO) {
                try {
                    taskListsApi.delete(taskListEntity.remoteId)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun renameTaskList(taskListId: Long, newTitle: String) {
        val now = clockNow()
        val taskListEntity = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
            .copy(
                title = newTitle,
                lastUpdateDate = now
            )
        taskListDao.upsert(taskListEntity)
        if (taskListEntity.remoteId != null) {
            val taskList = withContext(Dispatchers.IO) {
                try {
                    taskListsApi.update(
                        taskListEntity.remoteId,
                        TaskList(
                            id = taskListEntity.remoteId,
                            title = taskListEntity.title,
                            updatedDate = taskListEntity.lastUpdateDate
                        )
                    )
                } catch (_: Exception) {
                    null
                }
            }
            if (taskList != null) {
                taskListDao.upsert(taskList.asTaskListEntity(taskListId, taskListEntity.sorting))
            }
        }
    }

    suspend fun clearTaskListCompletedTasks(taskListId: Long) {
        val taskList = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
        // TODO local update date task list
        val completedTasks = taskDao.getCompletedTasks(taskListId)
        taskDao.deleteTasks(completedTasks.map(TaskEntity::id))
        if (taskList.remoteId != null) {
            coroutineScope {
                completedTasks.mapNotNull { task ->
                    if (task.remoteId == null) return@mapNotNull null
                    async(Dispatchers.IO) {
                        try {
                            // TODO deal with deleted locally but not remotely yet (no internet)
                            tasksApi.delete(taskList.remoteId, task.remoteId)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll()
            }
        }
    }

    suspend fun sortTasksBy(taskListId: Long, sorting: TaskListSorting) {
        val dbSorting = when (sorting) {
            TaskListSorting.Manual -> TaskListEntity.Sorting.UserDefined
            TaskListSorting.DueDate -> TaskListEntity.Sorting.DueDate
        }
        // no update date change, it's a local only information unrelated to remote tasks
        taskListDao.sortTasksBy(taskListId, dbSorting)
    }

    suspend fun createTask(taskListId: Long, title: String, notes: String = "", dueDate: Instant? = null) {
        val taskListEntity = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
        val now = clockNow()
        val firstPosition = 0.toTaskPosition()
        val currentTasks = taskDao.getTasksFromPositionOnward(taskListId, firstPosition)
            .toMutableList()
        val taskEntity = TaskEntity(
            parentListLocalId = taskListId,
            title = title,
            notes = notes,
            lastUpdateDate = now,
            dueDate = dueDate,
            position = firstPosition,
        )
        val taskId = taskDao.insert(taskEntity)
        if (currentTasks.isNotEmpty()) {
            currentTasks.add(0, taskEntity)
            val updatedTasks = computeTaskPositions(currentTasks)
            taskDao.upsertAll(updatedTasks - taskEntity)
        }

        if (taskListEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.insert(taskListEntity.remoteId, taskEntity.asTask())
                } catch (_: Exception) {
                    null
                }
            }
            if (task != null) {
                taskDao.upsert(task.asTaskEntity(taskListId, taskId))
            }
        }
    }

    suspend fun deleteTask(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        // TODO pending deletion?
        taskDao.deleteTask(taskId)
        // FIXME should already be available in entity, quick & dirty workaround
        val taskListRemoteId = taskEntity.parentTaskRemoteId
            ?: taskListDao.getById(taskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && taskEntity.remoteId != null) {
            withContext(Dispatchers.IO) {
                try {
                    tasksApi.delete(taskListRemoteId, taskEntity.remoteId)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun restoreTask(taskId: Long) {
        // TODO pending deletion
    }

    private suspend fun applyTaskUpdate(taskId: Long, updateLogic: suspend (TaskEntity, Instant) -> TaskEntity?) {
        val now = clockNow()
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val updatedTaskEntity = updateLogic(taskEntity, now) ?: return

        taskDao.upsert(updatedTaskEntity)

        // FIXME should already be available in entity, quick & dirty workaround
        val taskListRemoteId = updatedTaskEntity.parentTaskRemoteId
            ?: taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && updatedTaskEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.update(
                        taskListRemoteId,
                        updatedTaskEntity.remoteId,
                        updatedTaskEntity.asTask()
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (task != null) {
                taskDao.upsert(task.asTaskEntity(updatedTaskEntity.parentListLocalId, taskId))
            }
        }
    }

    suspend fun toggleTaskCompletionState(taskId: Long) {
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            // TODO should update position when changed/restored to not completed, what should it be?
            taskEntity.copy(
                isCompleted = !taskEntity.isCompleted,
                completionDate = if (taskEntity.isCompleted) null else updateTime,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun updateTask(taskListId: Long, taskId: Long, title: String, notes: String, dueDate: Instant?) {
        // TODO deal with task list update
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            taskEntity.copy(
                title = title,
                notes = notes,
                dueDate = dueDate,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun updateTaskTitle(taskId: Long, title: String) {
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            taskEntity.copy(
                title = title,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun updateTaskNotes(taskId: Long, notes: String) {
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            taskEntity.copy(
                notes = notes,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun updateTaskDueDate(taskId: Long, dueDate: Instant?) {
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            taskEntity.copy(
                dueDate = dueDate,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun indentTask(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val now = clockNow()
    }

    suspend fun unindentTask(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val now = clockNow()
    }

    suspend fun moveToTop(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        require(!taskEntity.isCompleted) { "Can't move completed tasks" }
        val now = clockNow()
        val updatedTaskEntity = taskEntity.copy(
            position = 0.toTaskPosition(),
            lastUpdateDate = now,
        )
        val tasksToUpdate = taskDao.getTasksUpToPosition(taskEntity.parentListLocalId, taskEntity.position)
            .toMutableList()
        tasksToUpdate.removeIf { it.id == taskEntity.id }
        // put the updated task at the beginning of the list to enforce proper ordering
        tasksToUpdate.add(0, updatedTaskEntity)
        val updatedTaskEntities = computeTaskPositions(tasksToUpdate)
        taskDao.upsertAll(updatedTaskEntities)

        // FIXME should already be available in entity, quick & dirty workaround
        val taskListRemoteId = updatedTaskEntity.parentTaskRemoteId
            ?: taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && updatedTaskEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.move(
                        taskListId = taskListRemoteId,
                        taskId = updatedTaskEntity.remoteId,
                        parentTaskId = null,
                        previousTaskId = null,
                        destinationTaskListId = null,
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (task != null) {
                taskDao.upsert(task.asTaskEntity(updatedTaskEntity.parentListLocalId, taskId))
            }
        }
    }

    suspend fun moveToList(taskId: Long, targetListId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val taskListEntity = requireNotNull(taskListDao.getById(targetListId)) { "Invalid task list id $targetListId" }
        val now = clockNow()
    }

    suspend fun moveToNewList(taskId: Long, newListTitle: String) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val now = clockNow()
        // TODO ideally transactional
    }
}
