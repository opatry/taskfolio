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
import net.opatry.tasks.data.util.inMemoryTasksAppDatabaseBuilder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskDaoTest {

    private val now: Instant
        get() = Clock.System.now()

    private lateinit var db: TasksAppDatabase
    private lateinit var taskDao: TaskDao

    @BeforeTest
    fun createDb() {
        db = inMemoryTasksAppDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .build()
        taskDao = db.getTaskDao()
        taskDao = db.getTaskDao()
    }

    @AfterTest
    fun closeDb() = db.close()

    @Test
    fun `when insert then getById task should return created TaskEntity`() = runTest {
        val giveNow = now
        val taskId = taskDao.insert(
            TaskEntity(
                title = "task",
                parentListLocalId = 0L,
                lastUpdateDate = giveNow,
                position = "00000000000000000000",
            )
        )

        val taskEntity = taskDao.getById(taskId)
        assertNotNull(taskEntity)
        assertEquals(taskId, taskEntity.id)
        assertEquals("task", taskEntity.title)
        assertEquals(giveNow, taskEntity.lastUpdateDate)
        assertEquals("00000000000000000000", taskEntity.position)
    }


    @Test
    fun `when insertAll local tasks then getLocalOnlyTasks should return all local tasks`() = runTest {
        val tasks = listOf(
            TaskEntity(
                id = 1L,
                parentListLocalId = 1L,
                title = "A",
                lastUpdateDate = now,
                position = "00000000000000000000"
            ),
            TaskEntity(
                id = 2L,
                parentListLocalId = 1L,
                title = "B",
                lastUpdateDate = now,
                position = "00000000000000000001"
            )
        )

        taskDao.insertAll(tasks)

        val localOnly = taskDao.getLocalOnlyTasks(1L)
        assertEquals(2, localOnly.size)
        assertEquals("00000000000000000000", localOnly[0].position)
        assertEquals("A", localOnly[0].title)
        assertEquals("00000000000000000001", localOnly[1].position)
        assertEquals("B", localOnly[1].title)
    }

    @Test
    fun `when upsert with updated title then getById taskId should return updated task`() = runTest {
        val original = TaskEntity(
            id = 1L,
            parentListLocalId = 1L,
            title = "Old",
            lastUpdateDate = now,
            position = "00000000000000000000"
        )
        taskDao.insert(original)

        taskDao.upsert(original.copy(title = "New"))

        val result = taskDao.getById(1L)
        assertNotNull(result)
        assertEquals("New", result.title)
    }

    @Test
    fun `when upsert with new task then getById taskId should return created task`() = runTest {
        val entity = TaskEntity(
            id = 1L,
            parentListLocalId = 1L,
            title = "Task",
            lastUpdateDate = now,
            position = "00000000000000000000"
        )

        taskDao.upsert(entity)

        val result = taskDao.getById(1L)
        assertNotNull(result)
        assertEquals("Task", result.title)
    }

    @Test
    fun `when upsertAll then getLocalOnlyTaskLists should return updated task lists`() = runTest {
        val items = listOf(
            TaskEntity(
                id = 1L,
                parentListLocalId = 1L,
                title = "One",
                lastUpdateDate = now,
                position = "00000000000000000000"
            ),
            TaskEntity(
                id = 2L,
                parentListLocalId = 1L,
                title = "Two",
                lastUpdateDate = now,
                position = "00000000000000000001"
            )
        )
        taskDao.insertAll(items)

        val updatedTasks = items.map {
            it.copy(title = "${it.title} 2")
        }
        taskDao.upsertAll(updatedTasks)

        val result = taskDao.getLocalOnlyTasks(1L)
        assertEquals(2, result.size)
        assertEquals("One 2", result[0].title)
        assertEquals("Two 2", result[1].title)
    }

    @Test
    fun `when deleteTask then getById taskId should return null`() = runTest {
        val task = TaskEntity(
            id = 1L,
            parentListLocalId = 1L,
            title = "ToDelete",
            lastUpdateDate = now,
            position = "00000000000000000000"
        )
        taskDao.insert(task)

        taskDao.deleteTask(1L)

        assertNull(taskDao.getById(1L))
    }

    @Test
    fun `when deleteTasks then getById taskId should return null for all deleted tasks`() = runTest {
        taskDao.insertAll(
            listOf(
                TaskEntity(
                    id = 1L,
                    parentListLocalId = 1L,
                    title = "A",
                    lastUpdateDate = now,
                    position = "00000000000000000000"
                ),
                TaskEntity(
                    id = 2L,
                    parentListLocalId = 1L,
                    title = "B",
                    lastUpdateDate = now,
                    position = "00000000000000000001"
                ),
            )
        )

        taskDao.deleteTasks(listOf(1L, 2L))

        assertNull(taskDao.getById(1L))
        assertNull(taskDao.getById(2L))
    }

    @Test
    fun `when getCompletedTasks then should return completed tasks only`() = runTest {
        taskDao.insertAll(
            listOf(
                TaskEntity(
                    id = 1L,
                    parentListLocalId = 1L,
                    title = "Done 1",
                    isCompleted = true,
                    lastUpdateDate = now,
                    position = "09999999999999999999"
                ),
                TaskEntity(
                    id = 2L,
                    parentListLocalId = 1L,
                    title = "Not done 1",
                    isCompleted = false,
                    lastUpdateDate = now,
                    position = "00000000000000000001"
                ),
                TaskEntity(
                    id = 3L,
                    parentListLocalId = 1L,
                    title = "Done 2",
                    isCompleted = true,
                    lastUpdateDate = now,
                    position = "09999999999999999998"
                ),
                TaskEntity(
                    id = 4L,
                    parentListLocalId = 1L,
                    title = "Not done 2",
                    isCompleted = false,
                    lastUpdateDate = now,
                    position = "00000000000000000003"
                ),
            )
        )

        val completed = taskDao.getCompletedTasks(1L)

        assertEquals(2, completed.size)
        assertTrue(completed.all(TaskEntity::isCompleted))
    }

    @Test
    fun `when deleteStaleTasks then should delete all tasks not in the list of valid remote ids`() = runTest {
        taskDao.insertAll(
            listOf(
                TaskEntity(
                    id = 1L,
                    parentListLocalId = 1L,
                    title = "Keep",
                    remoteId = "r1",
                    lastUpdateDate = now,
                    position = "00000000000000000000"
                ),
                TaskEntity(
                    id = 2L,
                    parentListLocalId = 1L,
                    title = "Delete",
                    remoteId = "r2",
                    lastUpdateDate = now,
                    position = "00000000000000000001"
                ),
            )
        )

        taskDao.deleteStaleTasks(1L, listOf("r1"))

        assertNull(taskDao.getByRemoteId("r2"))
        assertNotNull(taskDao.getByRemoteId("r1"))
    }

    @Test
    fun `when getAllAsFlow then should emit value for new tasks when collecting`() = runTest {
        val task = TaskEntity(
            id = 1L,
            parentListLocalId = 1L,
            title = "T",
            lastUpdateDate = now,
            position = "00000000000000000000"
        )

        taskDao.insert(task)

        val result = taskDao.getAllAsFlow().first()
        assertEquals(1, result.size)
        assertEquals("T", result[0].title)
    }

    @Test
    fun `given position p1 when getTasksUpToPosition then should return tasks with position lower or equal to p1`() = runTest {
        val firstTaskId = taskDao.insert(
            TaskEntity(
                title = "first",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000000",
            )
        )
        val secondTaskId = taskDao.insert(
            TaskEntity(
                title = "second",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000001",
            )
        )
        taskDao.insert(
            TaskEntity(
                title = "third",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000002",
            )
        )

        val tasks = taskDao.getTasksUpToPosition(0L, null, "00000000000000000001")

        assertEquals(2, tasks.size)
        assertEquals(firstTaskId, tasks[0].id)
        assertEquals("first", tasks[0].title)
        assertEquals("00000000000000000000", tasks[0].position)
        assertEquals(secondTaskId, tasks[1].id)
        assertEquals("second", tasks[1].title)
        assertEquals("00000000000000000001", tasks[1].position)
    }

    @Test
    fun `given position p1 when getTasksFromPositionOnward then should return tasks with position greater or equal to p1`() = runTest {
        taskDao.insert(
            TaskEntity(
                title = "first",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000000",
                parentTaskLocalId = null,
                isCompleted = false,
            )
        )
        val secondTaskId = taskDao.insert(
            TaskEntity(
                title = "second",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000001",
                parentTaskLocalId = null,
                isCompleted = false,
            )
        )
        val thirdTaskId = taskDao.insert(
            TaskEntity(
                title = "third",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000002",
                parentTaskLocalId = null,
                isCompleted = false,
            )
        )

        val tasks = taskDao.getTasksFromPositionOnward(0L, null, "00000000000000000001")

        assertEquals(2, tasks.size)
        assertEquals(secondTaskId, tasks[0].id)
        assertEquals("second", tasks[0].title)
        assertEquals("00000000000000000001", tasks[0].position)
        assertEquals(thirdTaskId, tasks[1].id)
        assertEquals("third", tasks[1].title)
        assertEquals("00000000000000000002", tasks[1].position)
    }

    @Test
    fun `when getTasksUpToPosition with no parent position then should return created TaskEntity`() = runTest {
        val now = Clock.System.now()
        val taskId = taskDao.insert(
            TaskEntity(
                title = "task",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000000",
            )
        )

        val tasks = taskDao.getTasksUpToPosition(0L, null, "00000000000000000000")

        assertEquals(1, tasks.size)
        assertEquals(taskId, tasks.first().id)
        assertEquals("task", tasks.first().title)
        assertEquals(now, tasks.first().lastUpdateDate)
        assertEquals("00000000000000000000", tasks.first().position)
    }

    @Test
    fun `when getTasksUpToPosition with parent position then should return created child TaskEntity`() = runTest {
        val parentTaskId = taskDao.insert(
            TaskEntity(
                title = "parent",
                parentListLocalId = 0L,
                lastUpdateDate = Clock.System.now(),
                position = "00000000000000000000",
            )
        )
        val now = Clock.System.now()
        val childTaskId = taskDao.insert(
            TaskEntity(
                title = "child",
                parentListLocalId = 0L,
                parentTaskLocalId = parentTaskId,
                lastUpdateDate = now,
                position = "00000000000000000000",
            )
        )

        val tasks = taskDao.getTasksUpToPosition(0L, parentTaskId, "00000000000000000000")

        assertEquals(1, tasks.size)
        assertEquals(childTaskId, tasks.first().id)
        assertEquals("child", tasks.first().title)
        assertEquals(now, tasks.first().lastUpdateDate)
        assertEquals("00000000000000000000", tasks.first().position)
    }

    @Test
    fun `when getTasksFromPositionOnward with no parent position then should return created TaskEntity`() = runTest {
        val now = Clock.System.now()
        val taskId = taskDao.insert(
            TaskEntity(
                title = "task",
                parentListLocalId = 0L,
                lastUpdateDate = now,
                position = "00000000000000000000",
            )
        )

        val tasks = taskDao.getTasksFromPositionOnward(0L, null, "00000000000000000000")

        assertEquals(1, tasks.size)
        assertEquals(taskId, tasks.first().id)
        assertEquals("task", tasks.first().title)
        assertEquals(now, tasks.first().lastUpdateDate)
        assertEquals("00000000000000000000", tasks.first().position)
    }

    @Test
    fun `when getTasksFromPositionOnward with parent position then should return created child TaskEntity`() = runTest {
        val parentTaskId = taskDao.insert(
            TaskEntity(
                title = "parent",
                parentListLocalId = 0L,
                lastUpdateDate = Clock.System.now(),
                position = "00000000000000000000",
            )
        )
        val now = Clock.System.now()
        val childTaskId = taskDao.insert(
            TaskEntity(
                title = "child",
                parentListLocalId = 0L,
                parentTaskLocalId = parentTaskId,
                lastUpdateDate = now,
                position = "00000000000000000000",
            )
        )

        val tasks = taskDao.getTasksFromPositionOnward(0L, parentTaskId, "00000000000000000000")

        assertEquals(1, tasks.size)
        assertEquals(childTaskId, tasks.first().id)
        assertEquals("child", tasks.first().title)
        assertEquals(now, tasks.first().lastUpdateDate)
        assertEquals("00000000000000000000", tasks.first().position)
    }

    @Test
    fun `when getPreviousSiblingTask task with mixed tasks and subtasks then should return root level task before task`() = runTest {
        val task1 = TaskEntity(
            parentListLocalId = 0L,
            title = "task1",
            position = "00000000000000000000",
            lastUpdateDate = now,
        )
        val task2 = task1.copy(
            title = "task2",
            position = "00000000000000000001",
        )
        val subtask1 = task1.copy(
            title = "subtask1",
            position = "00000000000000000000",
        )
        val subtask2 = task1.copy(
            title = "subtask2",
            position = "00000000000000000001",
        )
        val task1Id = taskDao.insert(task1)
        val insertedTask2 = taskDao.insert(task2)
            .let { taskDao.getById(it) }
            ?: error("Task not found")
        taskDao.insert(subtask1.copy(parentTaskLocalId = task1Id))
        taskDao.insert(subtask2.copy(parentTaskLocalId = task1Id))

        val result = taskDao.getPreviousSiblingTask(insertedTask2)

        assertNotNull(result)
        assertEquals(task1Id, result.id)
    }

    @Test
    fun `when getPreviousSiblingTask task without previous sibling then should return null`() = runTest {
        val task = TaskEntity(
            parentListLocalId = 0L,
            title = "task",
            position = "00000000000000000000",
            lastUpdateDate = now,
        )
        val insertedTask = taskDao.insert(task)
            .let { taskDao.getById(it) }
            ?: error("Task not found")

        val result = taskDao.getPreviousSiblingTask(insertedTask)

        assertNull(result)
    }

    @Test
    fun `when getPreviousSiblingTask subtask with mixed tasks and subtasks then should return indented task before task`() = runTest {
        val task1 = TaskEntity(
            parentListLocalId = 0L,
            title = "task1",
            position = "00000000000000000000",
            lastUpdateDate = now,
        )
        val task2 = task1.copy(
            title = "task2",
            position = "00000000000000000001",
        )
        val subtask1 = task1.copy(
            title = "subtask1",
            position = "00000000000000000000",
        )
        val subtask2 = task1.copy(
            title = "subtask2",
            position = "00000000000000000001",
        )

        val task1Id = taskDao.insert(task1)
        taskDao.insert(task2)
        val subtask1Id = taskDao.insert(subtask1.copy(parentTaskLocalId = task1Id))
        val insertedSubtask2 = taskDao.insert(subtask2.copy(parentTaskLocalId = task1Id))
            .let { taskDao.getById(it) }
            ?: error("Task not found")

        val result = taskDao.getPreviousSiblingTask(insertedSubtask2)

        assertNotNull(result)
        assertEquals(subtask1Id, result.id)
    }

    @Test
    fun `when getPreviousSiblingTask subtask without previous sibling then should return null`() = runTest {
        val task = TaskEntity(
            parentListLocalId = 0L,
            title = "task",
            position = "00000000000000000000",
            lastUpdateDate = now,
        )
        val subtask = task.copy(
            title = "subtask",
            position = "00000000000000000000",
        )
        val taskId = taskDao.insert(task)
        val insertedSubtask = taskDao.insert(subtask.copy(parentTaskLocalId = taskId))
            .let { taskDao.getById(it) }
            ?: error("Task not found")

        val result = taskDao.getPreviousSiblingTask(insertedSubtask)

        assertNull(result)
    }
}