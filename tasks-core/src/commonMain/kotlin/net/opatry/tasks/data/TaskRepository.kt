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
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.listAll
import net.opatry.tasks.NowProvider
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import java.math.BigInteger
import net.opatry.google.tasks.model.Task as RemoteTask
import net.opatry.google.tasks.model.TaskList as RemoteTaskList
import net.opatry.tasks.data.entity.TaskEntity as LocalTask
import net.opatry.tasks.data.entity.TaskListEntity as LocalTaskList

enum class TaskListSorting {
    Manual,
    DueDate,
    Title,
}

private fun RemoteTaskList.asTaskListEntity(localId: Long?, sorting: LocalTaskList.Sorting): LocalTaskList {
    return LocalTaskList(
        id = localId ?: 0,
        remoteId = id,
        etag = etag,
        title = title,
        lastUpdateDate = updatedDate,
        sorting = sorting,
    )
}

private fun RemoteTask.asTaskEntity(parentListLocalId: Long, parentTaskLocalId: Long?, taskLocalId: Long?): LocalTask {
    return LocalTask(
        id = taskLocalId ?: 0,
        remoteId = id,
        parentListLocalId = parentListLocalId,
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

private fun LocalTaskList.asTaskListDataModel(tasks: List<LocalTask>): TaskListDataModel {
    val parentIds = tasks.map(LocalTask::parentTaskLocalId).toSet()
    val (sorting, sortedTasks) = when (sorting) {
        LocalTaskList.Sorting.UserDefined -> TaskListSorting.Manual to sortTasksManualOrdering(tasks).map { (task, indent) ->
            task.asTaskDataModel(indent, !task.isCompleted && task.id in parentIds)
        }

        LocalTaskList.Sorting.DueDate -> TaskListSorting.DueDate to sortTasksDateOrdering(tasks).map { task ->
            task.asTaskDataModel(0, !task.isCompleted && task.id in parentIds)
        }

        LocalTaskList.Sorting.Title -> TaskListSorting.Title to sortTasksTitleOrdering(tasks).map { task ->
            task.asTaskDataModel(0, !task.isCompleted && task.id in parentIds)
        }
    }
    return TaskListDataModel(
        id = id,
        title = title,
        lastUpdate = lastUpdateDate,
        tasks = sortedTasks,
        sorting = sorting,
        // FIXME quick & dirty inferred hack to determine the default task list
        //  remote ID seems to be 32 chars for default list while it is 22 for others.
        // TODO add a column boolean is_default using TaskListsApi.default() for proper impl
        isDefault = if (remoteId != null) {
            remoteId.length > 22
        } else {
            id == 1L
        }
    )
}

private fun LocalTask.asTaskDataModel(indent: Int, isParentTask: Boolean): TaskDataModel {
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
        isParentTask = isParentTask,
    )
}

private fun LocalTask.asTask(): RemoteTask {
    return RemoteTask(
        id = remoteId ?: "",
        title = title,
        notes = notes,
        dueDate = dueDate,
        updatedDate = lastUpdateDate,
        status = if (isCompleted) RemoteTask.Status.Completed else RemoteTask.Status.NeedsAction,
        completedDate = completionDate,
        // doc says it's a read only field, but status is not hidden when syncing local only completed tasks
        // forcing the hidden status works and makes everything more consistent (position following 099999... pattern, hidden status)
        isHidden = isCompleted,
        position = position,
    )
}

fun sortTasksManualOrdering(tasks: List<LocalTask>): List<Pair<LocalTask, Int>> {
    // Step 1: Create a map of tasks by their IDs for easy lookup
    val taskMap = tasks.associateBy(LocalTask::id).toMutableMap()

    // Step 2: Build a tree structure with parent-child relationships
    val tree = mutableMapOf<Long, MutableList<LocalTask>>()
    tasks.forEach { task ->
        val parentId = task.parentTaskLocalId
        if (parentId == null) {
            // Tasks with no parent go directly into the root of the tree
            tree.getOrPut(task.id) { mutableListOf() }
        } else {
            // Add child task under its parent's list of children
            tree.getOrPut(parentId) { mutableListOf() }.add(task)
        }
    }

    // Step 3: Sort the child tasks by position
    tree.forEach { (_, children) ->
        children.sortBy(LocalTask::position)
    }

    // Step 4: Recursive function to traverse tasks and assign indentation levels
    fun traverseTasks(taskId: Long, level: Int, result: MutableList<Pair<LocalTask, Int>>) {
        val task = taskMap[taskId] ?: return
        result.add(task to level)
        val children = tree[taskId] ?: return
        for (child in children) {
            traverseTasks(child.id, level + 1, result)
        }
    }

    // Step 5: Start traversal from the root tasks (tasks with no parents)
    val sortedTasks = mutableListOf<Pair<LocalTask, Int>>()
    tree.keys.filter { taskMap[it]?.parentTaskRemoteId == null }.sortedBy { taskMap[it]?.position }.forEach {
        traverseTasks(it, 0, sortedTasks)
    }

    return sortedTasks
}

fun sortTasksDateOrdering(tasks: List<LocalTask>): List<LocalTask> {
    val (completedTasks, remainingTasks) = tasks.partition(LocalTask::isCompleted)
    val sortedRemainingTasks = remainingTasks.sortedWith(
        compareBy<LocalTask> { it.dueDate == null }
            .thenBy(LocalTask::dueDate)
    )
    return sortedRemainingTasks + sortCompletedTasks(completedTasks)
}

fun sortTasksTitleOrdering(tasks: List<LocalTask>): List<LocalTask> {
    val (completedTasks, remainingTasks) = tasks.partition(LocalTask::isCompleted)
    val sortedRemainingTasks = remainingTasks.sortedWith(
        compareBy<LocalTask> { it.title.lowercase() }
            .thenByDescending(LocalTask::title)
    )
    return sortedRemainingTasks + sortCompletedTasks(completedTasks)
}

fun sortCompletedTasks(tasks: List<LocalTask>): List<LocalTask> {
    require(tasks.all(LocalTask::isCompleted)) { "Only completed tasks can be sorted" }
    return tasks.sortedBy(LocalTask::position)
}

/**
 * Updates the position of tasks among the provided list of tasks.
 * The tasks to complete are kept in the same order and their position is recomputed starting from `newPositionStart`.
 * The completed tasks are sorted last and sorted by completion date.
 * Position is reset for each list & parent task.
 */
fun computeTaskPositions(tasks: List<LocalTask>, newPositionStart: Int = 0): List<LocalTask> {
    return buildList {
        val tasksByList = tasks.groupBy(LocalTask::parentListLocalId)
        tasksByList.forEach { (_, tasks) ->
            tasks.groupBy(LocalTask::parentTaskLocalId).forEach { (_, subTasks) ->
                val (completed, todo) = subTasks.partition { it.isCompleted && it.completionDate != null }
                val completedWithPositions = completed.map { it.copy(position = computeCompletedTaskPosition(it)) }
                val todoWithPositions = todo.mapIndexed { index, LocalTask ->
                    LocalTask.copy(position = (newPositionStart + index).toTaskPosition())
                }

                val sortedSubTasks = (todoWithPositions + completedWithPositions).sortedBy(LocalTask::position)
                addAll(sortedSubTasks)
            }
        }
    }
}

fun Number.toTaskPosition(): String = this.toString().padStart(20, '0')

fun computeCompletedTaskPosition(task: LocalTask): String {
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
    private val nowProvider: NowProvider,
) {
    // TODO persist it
    private var lastSync: Instant? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTaskLists() = taskListDao.getAllTaskListsWithTasksAsFlow()
        .distinctUntilChanged()
        .mapLatest { entries ->
            entries.map { (list, tasks) ->
                list.asTaskListDataModel(tasks)
            }
        }

    suspend fun cleanStaleTasks() {
        val remoteTaskListIds = withContext(Dispatchers.IO) {
            try {
                taskListsApi.listAll()
            } catch (_: Exception) {
                null
            }
        }?.map(RemoteTaskList::id) ?: return

        remoteTaskListIds.forEach { remoteTaskListId ->
            val localTaskListId = taskListDao.getByRemoteId(remoteTaskListId)?.id ?: return@forEach
            val remoteTaskIds = withContext(Dispatchers.IO) {
                tasksApi.listAll(
                    taskListId = remoteTaskListId,
                    showDeleted = true,
                    showHidden = true,
                    showCompleted = true,
                    updatedMin = null, // we need all content to purge local data accurately
                )
            }.map(RemoteTask::id)

            taskDao.deleteStaleTasks(localTaskListId, remoteTaskIds)
        }

        taskListDao.deleteStaleTaskLists(remoteTaskListIds)
    }

    suspend fun sync(cleanStaleTasks: Boolean = lastSync == null) {
        val remoteTaskLists = withContext(Dispatchers.IO) {
            try {
                taskListsApi.listAll()
            } catch (_: Exception) {
                null
            }
        } ?: return // most likely not internet, can't fetch data, nothing to sync

        // update local lists from remote counterparts
        val syncedTaskLists = remoteTaskLists.map { remoteTaskList ->
            val existingLocalList = taskListDao.getByRemoteId(remoteTaskList.id)
            remoteTaskList.asTaskListEntity(
                localId = existingLocalList?.id,
                sorting = existingLocalList?.sorting ?: LocalTaskList.Sorting.UserDefined
            )
        }.takeUnless(List<*>::isEmpty)?.let { lists ->
            val finalListIds = taskListDao.upsertAll(lists)
            // update list ids following upsertAll
            lists.zip(finalListIds) { list, finalId -> list.copy(id = finalId) }
        } ?: emptyList()

        // pull remote tasks
        syncedTaskLists.flatMap { taskList ->
            taskList.remoteId?.let { remoteTaskListId ->
                pullRemoteTasks(taskList.id, remoteTaskListId)
            } ?: emptyList()
        }.also { syncedTasks ->
            if (syncedTasks.isNotEmpty()) {
                taskDao.upsertAll(syncedTasks)
            }
        }

        // push local only lists
        val localOnlySyncedTaskLists = taskListDao.getLocalOnlyTaskLists().mapNotNull { localTaskList ->
            pushLocalTaskList(localTaskList)
        }.takeUnless(List<*>::isEmpty)?.let { syncedTaskLists ->
            val finalListIds = taskListDao.upsertAll(syncedTaskLists)
            // update list ids following upsertAll
            syncedTaskLists.zip(finalListIds) { list, finalId -> list.copy(id = finalId) }
        } ?: emptyList()

        // push local only tasks
        val allTaskLists = syncedTaskLists + localOnlySyncedTaskLists
        allTaskLists.flatMap { taskList ->
            val localOnlyTasks = taskDao.getLocalOnlyTasks(taskList.id)
            taskList.remoteId?.let { remoteTaskListId ->
                pushLocalTasks(
                    localTaskListId = taskList.id,
                    remoteTaskListId = remoteTaskListId,
                    localParentTaskId = null,
                    remoteParentTaskId = null,
                    localOnlyTasks,
                )
            } ?: emptyList()
        }.also { syncedTasks ->
            if (syncedTasks.isNotEmpty()) {
                taskDao.upsertAll(syncedTasks)
            }
        }

        lastSync = nowProvider.now()

        if (cleanStaleTasks) {
            cleanStaleTasks()
        }
    }

    private suspend fun pushLocalTaskList(localTaskList: LocalTaskList): LocalTaskList? {
        return withContext(Dispatchers.IO) {
            try {
                taskListsApi.insert(RemoteTaskList(localTaskList.title))
            } catch (_: Exception) {
                null
            }
        }?.asTaskListEntity(localTaskList.id, localTaskList.sorting)
    }

    private suspend fun pullRemoteTasks(localTaskListId: Long, remoteTaskListId: String): List<LocalTask> {
        return withContext(Dispatchers.IO) {
            tasksApi.listAll(
                taskListId = remoteTaskListId,
                showDeleted = false,
                showHidden = true,
                showCompleted = true,
                updatedMin = lastSync,
            )
        }.map { remoteTask ->
            val existingLocalTask = taskDao.getByRemoteId(remoteTask.id)
            val localParentTask = remoteTask.parent?.let { taskDao.getByRemoteId(it) }
            remoteTask.asTaskEntity(
                parentListLocalId = localTaskListId,
                parentTaskLocalId = localParentTask?.id,
                taskLocalId = existingLocalTask?.id,
            )
        }
    }

    private suspend fun pushLocalTasks(
        localTaskListId: Long,
        remoteTaskListId: String,
        localParentTaskId: Long?,
        remoteParentTaskId: String?,
        tasks: List<LocalTask>
    ): List<LocalTask> {
        val tasksToSync = computeTaskPositions(tasks.filter { it.parentTaskLocalId == localParentTaskId })
        var previousTaskId: String? = null
        return buildList {
            tasksToSync.onEach { localTask ->
                val remoteTask = withContext(Dispatchers.IO) {
                    try {
                        tasksApi.insert(
                            taskListId = remoteTaskListId,
                            task = localTask.asTask(),
                            parentTaskId = remoteParentTaskId,
                            previousTaskId = previousTaskId,
                        ).also { remoteTask ->
                            // ensure up to date local parent after sync
                            val localParentTask = remoteTask.parent?.let { taskDao.getByRemoteId(it) }
                            add(
                                remoteTask.asTaskEntity(
                                    parentListLocalId = localTaskListId,
                                    parentTaskLocalId = localParentTask?.id,
                                    taskLocalId = localTask.id,
                                )
                            )
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                // FIXME if one of the task sync fails, it breaks sibling order
                previousTaskId = remoteTask?.id

                // don't try syncing sub tasks if parent task failed, it would break hierarchy on remote side
                if (remoteTask != null) {
                    addAll(
                        pushLocalTasks(
                            localTaskListId = localTaskListId,
                            remoteTaskListId = remoteTaskListId,
                            localParentTaskId = localTask.id,
                            remoteParentTaskId = remoteTask.id,
                            tasks = tasks,
                        )
                    )
                }
            }
        }
    }

    suspend fun createTaskList(title: String): Long {
        val now = nowProvider.now()
        val taskListId = taskListDao.insert(LocalTaskList(title = title, lastUpdateDate = now))
        val taskList = withContext(Dispatchers.IO) {
            try {
                taskListsApi.insert(RemoteTaskList(title = title, updatedDate = now))
            } catch (_: Exception) {
                null
            }
        }
        if (taskList != null) {
            taskListDao.upsert(taskList.asTaskListEntity(taskListId, LocalTaskList.Sorting.UserDefined))
        }
        return taskListId
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
        val now = nowProvider.now()
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
                        RemoteTaskList(
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
        taskDao.deleteTasks(completedTasks.map(LocalTask::id))
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
            TaskListSorting.Manual -> LocalTaskList.Sorting.UserDefined
            TaskListSorting.DueDate -> LocalTaskList.Sorting.DueDate
            TaskListSorting.Title -> LocalTaskList.Sorting.Title
        }
        // no update date change, it's a local only information unrelated to remote tasks
        taskListDao.sortTasksBy(taskListId, dbSorting)
    }

    suspend fun createTask(taskListId: Long, parentTaskId: Long? = null, title: String, notes: String = "", dueDate: Instant? = null): Long {
        val taskListEntity = requireNotNull(taskListDao.getById(taskListId)) { "Invalid task list id $taskListId" }
        val parentTaskEntity = parentTaskId?.let { requireNotNull(taskDao.getById(it)) { "Invalid parent task id $parentTaskId" } }
        val now = nowProvider.now()
        val firstPosition = 0.toTaskPosition()
        val currentTasks = taskDao.getTasksFromPositionOnward(taskListId, parentTaskId, firstPosition)
            .toMutableList()
        val taskEntity = LocalTask(
            parentListLocalId = taskListId,
            parentTaskLocalId = parentTaskId,
            title = title,
            notes = notes,
            lastUpdateDate = now,
            dueDate = dueDate,
            position = firstPosition,
        )
        val taskId = taskDao.insert(taskEntity)
        if (currentTasks.isNotEmpty()) {
            val updatedTasks = computeTaskPositions(currentTasks, newPositionStart = 1)
            taskDao.upsertAll(updatedTasks)
        }

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
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = taskListId,
                        parentTaskLocalId = parentTaskId,
                        taskLocalId = taskId,
                    )
                )
            }
        }
        return taskId
    }

    suspend fun deleteTask(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        // TODO pending deletion?
        taskDao.deleteTask(taskId)

        val tasksToUpdated = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, taskEntity.parentTaskLocalId, taskEntity.position)
        if (tasksToUpdated.isNotEmpty()) {
            val updatedTasks = computeTaskPositions(tasksToUpdated, newPositionStart = taskEntity.position.toInt())
            taskDao.upsertAll(updatedTasks)
        }

        val taskListRemoteId = taskListDao.getById(taskEntity.parentListLocalId)?.remoteId
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

    private suspend fun applyTaskUpdate(taskId: Long, updateLogic: suspend (LocalTask, Instant) -> LocalTask?) {
        val now = nowProvider.now()
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val updatedTaskEntity = updateLogic(taskEntity, now) ?: return

        taskDao.upsert(updatedTaskEntity)

        val taskListRemoteId = taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
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
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = updatedTaskEntity.parentListLocalId,
                        parentTaskLocalId = updatedTaskEntity.parentTaskLocalId,
                        taskLocalId = taskId,
                    )
                )
            }
        }
    }

    suspend fun toggleTaskCompletionState(taskId: Long) {
        applyTaskUpdate(taskId) { taskEntity, updateTime ->
            // TODO should update position when changed/restored to not completed, what should it be?
            val isNowCompleted = !taskEntity.isCompleted
            taskEntity.copy(
                isCompleted = isNowCompleted,
                completionDate = if (isNowCompleted) updateTime else null,
                lastUpdateDate = updateTime,
            )
        }
    }

    suspend fun updateTask(taskId: Long, title: String, notes: String, dueDate: Instant?) {
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
        require(taskEntity.parentTaskLocalId == null) { "Cannot indent subtask" }
        val parentTaskEntity = requireNotNull(taskDao.getPreviousSiblingTask(taskEntity)) {
            "Cannot indent top level task at first position"
        }
        require(parentTaskEntity.parentTaskLocalId == null) { "Parent task must be a top level task" }

        val subTasks = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, taskEntity.id, 0.toTaskPosition())
        require(subTasks.isEmpty()) { "Cannot indent task with subtasks" }

        val now = nowProvider.now()
        val targetPosition = Int.MAX_VALUE.toTaskPosition()
        val updatedTaskEntity = taskEntity.copy(
            parentTaskLocalId = parentTaskEntity.id,
            lastUpdateDate = now,
            position = targetPosition,
        )

        // compute final subtasks position and find previous sibling subtask
        val subtasksToUpdate = taskDao.getTasksUpToPosition(taskEntity.parentListLocalId, parentTaskEntity.id, targetPosition)
            .toMutableList()
        val previousSubtaskRemoteId = subtasksToUpdate.lastOrNull()?.remoteId
        subtasksToUpdate += updatedTaskEntity
        val updatedSubtaskEntities = computeTaskPositions(subtasksToUpdate)
        taskDao.upsertAll(updatedSubtaskEntities)

        // indent tasks position after the indented task
        val tasksToUpdate = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, null, taskEntity.position)
        if (tasksToUpdate.isNotEmpty()) {
            val updatedTaskEntities = computeTaskPositions(tasksToUpdate, taskEntity.position.toInt())
            taskDao.upsertAll(updatedTaskEntities)
        }

        val taskListRemoteId = taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && updatedTaskEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.move(
                        taskListId = taskListRemoteId,
                        taskId = updatedTaskEntity.remoteId,
                        parentTaskId = parentTaskEntity.remoteId,
                        previousTaskId = previousSubtaskRemoteId,
                        destinationTaskListId = null,
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (task != null) {
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = updatedTaskEntity.parentListLocalId,
                        parentTaskLocalId = parentTaskEntity.id,
                        taskLocalId = updatedTaskEntity.id,
                    )
                )
            }
        }
    }

    suspend fun unindentTask(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val parentTaskId = requireNotNull(taskEntity.parentTaskLocalId) { "Cannot unindent top level task" }
        val parentTaskEntity = requireNotNull(taskDao.getById(parentTaskId)) { "Invalid parent task id ${taskEntity.parentTaskLocalId}" }

        val now = nowProvider.now()
        val parentTaskPosition = parentTaskEntity.position
        val newPosition = parentTaskPosition.toInt() + 1

        val updatedTaskEntity = taskEntity.copy(
            parentTaskLocalId = null,
            lastUpdateDate = now,
            position = newPosition.toTaskPosition(),
        )

        // compute final subtasks position
        val subtasksToUpdate = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, parentTaskEntity.id, taskEntity.position)
            .toMutableList()
        subtasksToUpdate.removeIf { it.id == taskEntity.id }
        val updatedSubtaskEntities = computeTaskPositions(subtasksToUpdate, taskEntity.position.toInt())
        taskDao.upsertAll(updatedSubtaskEntities)

        // compute final tasks position
        val tasksToUpdate = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, null, updatedTaskEntity.position)
            .toMutableList()
        // put the updated task at the beginning of the list to enforce proper ordering
        tasksToUpdate.add(0, updatedTaskEntity)
        val updatedTaskEntities = computeTaskPositions(tasksToUpdate, newPositionStart = newPosition)
        taskDao.upsertAll(updatedTaskEntities)

        val taskListRemoteId = taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && updatedTaskEntity.remoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.move(
                        taskListId = taskListRemoteId,
                        taskId = updatedTaskEntity.remoteId,
                        parentTaskId = null,
                        previousTaskId = parentTaskEntity.remoteId,
                        destinationTaskListId = null,
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (task != null) {
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = updatedTaskEntity.parentListLocalId,
                        parentTaskLocalId = null,
                        taskLocalId = updatedTaskEntity.id,
                    )
                )
            }
        }
    }

    suspend fun moveToTop(taskId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        require(!taskEntity.isCompleted) { "Can't move completed tasks" }
        val now = nowProvider.now()
        val updatedTaskEntity = taskEntity.copy(
            position = 0.toTaskPosition(),
            lastUpdateDate = now,
        )
        val tasksToUpdate = taskDao.getTasksUpToPosition(taskEntity.parentListLocalId, null, taskEntity.position)
            .toMutableList()
        tasksToUpdate.removeIf { it.id == taskEntity.id }
        // put the updated task at the beginning of the list to enforce proper ordering
        tasksToUpdate.add(0, updatedTaskEntity)
        val updatedTaskEntities = computeTaskPositions(tasksToUpdate)
        taskDao.upsertAll(updatedTaskEntities)

        val taskListRemoteId = taskListDao.getById(updatedTaskEntity.parentListLocalId)?.remoteId
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
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = updatedTaskEntity.parentListLocalId,
                        parentTaskLocalId = null,
                        taskLocalId = taskId,
                    )
                )
            }
        }
    }

    suspend fun moveToList(taskId: Long, destinationListId: Long) {
        val taskEntity = requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }
        val destinationTaskListEntity = requireNotNull(taskListDao.getById(destinationListId)) { "Invalid task list id $destinationListId" }

        val now = nowProvider.now()
        val updatedTaskEntity = taskEntity.copy(
            parentListLocalId = destinationListId,
            lastUpdateDate = now,
            position = 0.toTaskPosition(),
        )
        taskDao.upsert(updatedTaskEntity)

        // update positions of source list
        val initialTasksAfter = taskDao.getTasksFromPositionOnward(taskEntity.parentListLocalId, null, taskEntity.position)
            .toMutableList()
        if (initialTasksAfter.isNotEmpty()) {
            val updatedTasks = computeTaskPositions(initialTasksAfter, taskEntity.position.toInt())
            taskDao.upsertAll(updatedTasks)
        }

        // update positions of destination list
        val destinationTasksAfter = taskDao.getTasksUpToPosition(updatedTaskEntity.id, null, updatedTaskEntity.position)
            .toMutableList()
        destinationTasksAfter.removeIf { it.id == updatedTaskEntity.id }
        if (destinationTasksAfter.isNotEmpty()) {
            val updatedTaskEntities = computeTaskPositions(destinationTasksAfter, newPositionStart = 1)
            taskDao.upsertAll(updatedTaskEntities)
        }

        // FIXME should already be available in entity, quick & dirty workaround
        val newTaskListRemoteId = destinationTaskListEntity.remoteId
        val taskListRemoteId = taskListDao.getById(taskEntity.parentListLocalId)?.remoteId
        if (taskListRemoteId != null && taskEntity.remoteId != null && newTaskListRemoteId != null) {
            val task = withContext(Dispatchers.IO) {
                try {
                    tasksApi.move(
                        taskListId = taskListRemoteId,
                        taskId = taskEntity.remoteId,
                        parentTaskId = null,
                        previousTaskId = null,
                        destinationTaskListId = newTaskListRemoteId,
                    )
                } catch (_: Exception) {
                    null
                }
            }

            if (task != null) {
                taskDao.upsert(
                    task.asTaskEntity(
                        parentListLocalId = destinationListId,
                        parentTaskLocalId = null,
                        taskLocalId = taskId,
                    )
                )
            }
        }
    }

    suspend fun moveToNewList(taskId: Long, newListTitle: String): Long {
        requireNotNull(taskDao.getById(taskId)) { "Invalid task id $taskId" }

        // TODO ideally transactional
        val newTaskListId = createTaskList(newListTitle)
        moveToList(taskId, newTaskListId)
        return newTaskListId
    }
}
