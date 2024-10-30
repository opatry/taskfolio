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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity

@Dao
interface TaskListDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TaskListEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<TaskListEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaskListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskListEntity>): List<Long>

    // FIXME should be a pending deletion "flag" until sync is done
    @Query("DELETE FROM task_list WHERE local_id = :id")
    suspend fun deleteTaskList(id: Long)

    @Query("SELECT * FROM task_list WHERE local_id = :id")
    suspend fun getById(id: Long): TaskListEntity?

    @Query("SELECT * FROM task_list WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): TaskListEntity?

    @Query("SELECT * FROM task_list")
    fun getAllAsFlow(): Flow<List<TaskListEntity>>

    // FIXME order should use "parent" lexicographic order
    // use LEFT JOIN to get all task lists even if they have no task
    @Query(
        """SELECT * FROM task_list
LEFT JOIN task ON task_list.local_id = task.parent_list_local_id ORDER BY task_list.local_id ASC, task.local_id ASC"""
    )
    fun getAllTaskListsWithTasksAsFlow(): Flow<Map<TaskListEntity, List<TaskEntity>>>

    @Query("SELECT * FROM task_list WHERE remote_id IS NULL")
    suspend fun getLocalOnlyTaskLists(): List<TaskListEntity>

    @Query("DELETE FROM task_list WHERE remote_id IS NOT NULL AND remote_id NOT IN (:validRemoteIds)")
    suspend fun deleteStaleTaskLists(validRemoteIds: List<String>)

    @Query("UPDATE task_list SET sorting = :sorting WHERE local_id = :taskListId")
    suspend fun sortTasksBy(taskListId: Long, sorting: TaskListEntity.Sorting)
}