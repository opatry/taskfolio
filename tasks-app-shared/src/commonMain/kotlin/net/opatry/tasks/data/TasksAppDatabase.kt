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

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity


object Converters {
    @TypeConverter
    fun instantFromString(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun instantToString(instant: Instant?): String? = instant?.toString()
}

@Database(
    entities = [
        TaskListEntity::class,
        TaskEntity::class,
    ],
    version = 1
)
@ConstructedBy(TasksAppDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class TasksAppDatabase : RoomDatabase() {
    abstract fun getTaskListDao(): TaskListDao
    abstract fun getTaskDao(): TaskDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object TasksAppDatabaseConstructor : RoomDatabaseConstructor<TasksAppDatabase> {
    override fun initialize(): TasksAppDatabase
}

@Dao
interface TaskListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaskListEntity): Long

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
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaskEntity): Long

    @Query("SELECT * FROM task WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): TaskEntity?

    @Query("SELECT * FROM task")
    fun getAllAsFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task WHERE remote_id IS NULL")
    suspend fun getLocalOnlyTasks(): List<TaskEntity>

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
}
