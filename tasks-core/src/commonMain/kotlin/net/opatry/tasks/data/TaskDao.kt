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


@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaskEntity): Long

    @Query("SELECT * FROM task WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): TaskEntity?

    @Query("SELECT * FROM task")
    fun getAllAsFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE parent_list_local_id = :taskListLocalId AND remote_id IS NULL")
    suspend fun getLocalOnlyTasks(taskListLocalId: Long): List<TaskEntity>

    // FIXME should be a pending deletion "flag" until sync is done
    @Query("DELETE FROM task WHERE local_id = :id")
    suspend fun deleteTask(id: Long)

    // FIXME should be a pending deletion "flag" until sync is done
    @Query("DELETE FROM task WHERE local_id IN (:ids)")
    suspend fun deleteTasks(ids: List<Long>)

    @Query("SELECT * FROM task WHERE parent_list_local_id = :taskListLocalId AND is_completed = true")
    suspend fun getCompletedTasks(taskListLocalId: Long): List<TaskEntity>

    @Query("DELETE FROM task WHERE parent_list_local_id = :taskListId AND remote_id IS NOT NULL AND remote_id NOT IN (:validRemoteIds)")
    suspend fun deleteStaleTasks(taskListId: Long, validRemoteIds: List<String>)

    @Query("SELECT * FROM task WHERE local_id = :id")
    suspend fun getById(id: Long): TaskEntity?
}
