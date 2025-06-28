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

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.util.inMemoryTasksAppDatabaseBuilder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskListDaoTest {

    private val now: Instant
        get() = Clock.System.now()

    private lateinit var db: TasksAppDatabase
    private lateinit var taskListDao: TaskListDao
    private lateinit var taskDao: TaskDao

    @BeforeTest
    fun createDb() {
        db = inMemoryTasksAppDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .build()
        taskListDao = db.getTaskListDao()
        taskDao = db.getTaskDao()
    }

    @AfterTest
    fun closeDb() = db.close()

    @Test
    fun `when insert then GetById should return the created task list`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "Test",
            lastUpdateDate = now,
        )

        taskListDao.insert(entity)

        val result = taskListDao.getById(1L)
        assertNotNull(result)
        assertEquals("Test", result.title)
    }

    @Test
    fun `when insertAll local task lists then getLocalOnlyTaskLists should return all local task lists`() = runTest {
        val items = listOf(
            TaskListEntity(
                id = 1L,
                title = "One",
                lastUpdateDate = now,
            ),
            TaskListEntity(
                id = 2L,
                title = "Two",
                lastUpdateDate = now,
            )
        )

        taskListDao.insertAll(items)

        val result = taskListDao.getLocalOnlyTaskLists()
        assertEquals(2, result.size)
        assertEquals("One", result[0].title)
        assertEquals("Two", result[1].title)
    }

    @Test
    fun `when upsert with updated title then getById should return updated task list`() = runTest {
        val original = TaskListEntity(
            id = 1L,
            title = "Old",
            lastUpdateDate = now,
        )
        taskListDao.insert(original)

        taskListDao.upsert(original.copy(title = "New"))

        val result = taskListDao.getById(1L)
        assertNotNull(result)
        assertEquals("New", result.title)
    }

    @Test
    fun `when upsert with new task list then getById should return created task list`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "List",
            lastUpdateDate = now,
        )

        taskListDao.upsert(entity)

        val result = taskListDao.getById(1L)
        assertNotNull(result)
        assertEquals("List", result.title)
    }

    @Test
    fun `when upsertAll then getLocalOnlyTaskLists should return updated task lists`() = runTest {
        val items = listOf(
            TaskListEntity(
                id = 1L,
                title = "One",
                lastUpdateDate = now,
            ),
            TaskListEntity(
                id = 2L,
                title = "Two",
                lastUpdateDate = now,
            )
        )
        taskListDao.insertAll(items)

        val updatedTasks = items.map {
            it.copy(title = "${it.title} 2")
        }
        taskListDao.upsertAll(updatedTasks)

        val result = taskListDao.getLocalOnlyTaskLists()
        assertEquals(2, result.size)
        assertEquals("One 2", result[0].title)
        assertEquals("Two 2", result[1].title)
    }

    @Test
    fun `when deleteTaskList then getById should return null`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "ToDelete",
            lastUpdateDate = now,
        )
        taskListDao.insert(entity)

        taskListDao.deleteTaskList(1L)

        val result = taskListDao.getById(1L)
        assertNull(result)
    }

    @Test
    fun `when deleteStaleTaskLists then should delete all task lists not in the list of valid remote ids`() = runTest {
        taskListDao.insertAll(
            listOf(
                TaskListEntity(
                    id = 1L,
                    remoteId = "valid1",
                    title = "Keep",
                    lastUpdateDate = now,
                ),
                TaskListEntity(
                    id = 2L,
                    remoteId = "stale",
                    title = "Remove",
                    lastUpdateDate = now,
                )
            )
        )

        taskListDao.deleteStaleTaskLists(listOf("valid1"))

        val valid = taskListDao.getByRemoteId("valid1")
        assertNotNull(valid)
        val stale = taskListDao.getByRemoteId("stale")
        assertNull(stale)
    }

    @Test
    fun `when sortTasksBy DueDate then sorting should be updated to DueDate`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "Sortable",
            lastUpdateDate = now,
            sorting = TaskListEntity.Sorting.UserDefined
        )
        taskListDao.insert(entity)

        taskListDao.sortTasksBy(1L, TaskListEntity.Sorting.DueDate)

        val result = taskListDao.getById(1L)
        assertNotNull(result)
        assertEquals(TaskListEntity.Sorting.DueDate, result.sorting)
    }

    @Test
    fun `when sortTasksBy UserDefined then sorting should be updated to UserDefined`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "Sortable",
            lastUpdateDate = now,
            sorting = TaskListEntity.Sorting.DueDate
        )
        taskListDao.insert(entity)

        taskListDao.sortTasksBy(1L, TaskListEntity.Sorting.UserDefined)

        val result = taskListDao.getById(1L)
        assertNotNull(result)
        assertEquals(TaskListEntity.Sorting.UserDefined, result.sorting)
    }

    @Test
    fun `when getAllAsFlow then should emit value for new task lists when collecting`() = runTest {
        val entity = TaskListEntity(
            id = 1L,
            title = "List",
            lastUpdateDate = now,
        )
        taskListDao.insert(entity)

        val result = taskListDao.getAllAsFlow().first()
        assertEquals(1, result.size)
        assertEquals("List", result[0].title)
    }

    @Test
    fun `when getAllTaskListsWithTasksAsFlow then should return task lists with tasks in order`() = runTest {
        val taskList1 = TaskListEntity(
            id = 1L,
            title = "List 1",
            lastUpdateDate = now
        )
        val taskList2 = TaskListEntity(
            id = 2L,
            title = "List 2",
            lastUpdateDate = now
        )
        val task1 = TaskEntity(
            id = 1L,
            parentListLocalId = 1L,
            title = "Task 1",
            lastUpdateDate = now,
            position = "00000000000000000000"
        )
        val task2 = TaskEntity(
            id = 2L,
            parentListLocalId = 1L,
            title = "Task 2",
            lastUpdateDate = now,
            position = "00000000000000000001"
        )
        taskListDao.insertAll(listOf(taskList1, taskList2))
        taskDao.insertAll(listOf(task1, task2))

        val result = taskListDao.getAllTaskListsWithTasksAsFlow().first()

        assertEquals(2, result.size)
        val tasks1 = result[taskList1]
        assertNotNull(tasks1)
        assertEquals(listOf(1L, 2L), tasks1.map(TaskEntity::id))
        assertEquals(listOf("Task 1", "Task 2"), tasks1.map(TaskEntity::title))
        val tasks2 = result[taskList2]
        assertNotNull(tasks2)
        assertEquals(emptyList(), tasks2)
    }
}