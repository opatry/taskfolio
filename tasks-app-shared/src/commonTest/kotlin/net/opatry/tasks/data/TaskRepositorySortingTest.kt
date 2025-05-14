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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.util.runTaskRepositoryTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days

class TaskRepositorySortingTest {
    private val now: Instant
        get() = Clock.System.now()

    @Test
    fun `sort tasks by due date with nulls put last`() = runTaskRepositoryTest { repository ->
        val task1 = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "Task 1",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )
        val task2 = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "Task 1",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksDateOrdering(listOf(task1, task2))

        assertEquals(2, tasks.size)
        assertEquals(task2.id, tasks[0].id)
        assertEquals(task1.id, tasks[1].id)
    }

    @Test
    fun `sort tasks by due date put remaining tasks before completed tasks`() {
        val completedTask = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "task",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now,
            position = "09999999999999999999",
        )
        val task = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "task",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksDateOrdering(listOf(completedTask, task))

        assertEquals(2, tasks.size)
        assertEquals(task.id, tasks[0].id)
        assertEquals(completedTask.id, tasks[1].id)
    }

    @Test
    fun `sort tasks by due date still sorts completed tasks by position`() {
        val completedTaskFirst = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "task",
            dueDate = now + 1.days,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now - 1.days,
            position = "09999999999999999999",
        )
        val completedTaskLast = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "task",
            dueDate = now - 1.days,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now,
            position = "09999999999999999998",
        )
        val task = TaskEntity(
            id = 3,
            parentListLocalId = 1,
            title = "task",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksDateOrdering(listOf(completedTaskLast, completedTaskFirst, task))

        assertEquals(3, tasks.size)
        assertEquals(task.id, tasks[0].id)
        assertEquals(completedTaskLast.id, tasks[1].id)
        assertEquals(completedTaskFirst.id, tasks[2].id)
    }

    @Test
    fun `sort tasks by title ignore case`() = runTaskRepositoryTest { repository ->
        val task1Lower = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "t1",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )
        val task1Upper = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "T11",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )
        val task2Lower = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "t2",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )
        val task2Upper = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "T22",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksTitleOrdering(listOf(task1Lower, task2Lower, task1Upper, task2Upper))

        assertEquals(4, tasks.size)
        assertEquals(task1Lower.id, tasks[0].id)
        assertEquals(task1Upper.id, tasks[1].id)
        assertEquals(task2Lower.id, tasks[2].id)
        assertEquals(task2Upper.id, tasks[3].id)
    }

    @Test
    fun `sort tasks by title ignore case and fallbacks to lower before upper if same`() = runTaskRepositoryTest { repository ->
        val task1Lower = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "t1",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )
        val task1Upper = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "T1",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksTitleOrdering(listOf(task1Upper, task1Lower))

        assertEquals(2, tasks.size)
        assertEquals(task1Lower.id, tasks[0].id)
        assertEquals(task1Upper.id, tasks[1].id)
    }

    @Test
    fun `sort tasks by title put remaining tasks before completed tasks`() {
        val completedTask = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "aaa",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now,
            position = "09999999999999999999",
        )
        val task = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "bbb",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksTitleOrdering(listOf(completedTask, task))

        assertEquals(2, tasks.size)
        assertEquals(task.id, tasks[0].id)
        assertEquals(completedTask.id, tasks[1].id)
    }

    @Test
    fun `sort tasks by title still sorts completed tasks by position`() {
        val completedTaskFirst = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "aaa",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now - 1.days,
            position = "09999999999999999999",
        )
        val completedTaskLast = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "bbb",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now,
            position = "09999999999999999998",
        )
        val task = TaskEntity(
            id = 3,
            parentListLocalId = 1,
            title = "task",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = false,
            position = "00000000000000000000",
        )

        val tasks = sortTasksTitleOrdering(listOf(completedTaskLast, completedTaskFirst, task))

        assertEquals(3, tasks.size)
        assertEquals(task.id, tasks[0].id)
        assertEquals(completedTaskLast.id, tasks[1].id)
        assertEquals(completedTaskFirst.id, tasks[2].id)
    }

    @Test
    fun `sorting completed tasks should throw IllegalArgumentException when providing uncompleted tasks`() {
        assertFailsWith<IllegalArgumentException>("Only completed tasks can be sorted") {
            sortCompletedTasks(
                listOf(
                    TaskEntity(
                        id = 1,
                        parentListLocalId = 1,
                        title = "task",
                        dueDate = now,
                        lastUpdateDate = now,
                        isCompleted = false,
                        position = "00000000000000000000",
                    ),
                    TaskEntity(
                        id = 2,
                        parentListLocalId = 1,
                        title = "task",
                        dueDate = null,
                        lastUpdateDate = now,
                        isCompleted = true,
                        completionDate = now,
                        position = "09999999999999999999",
                    ),
                )
            )
        }
    }

    @Test
    fun `sorting completed tasks should sort last completed tasks first`() {
        val completedTaskFirst = TaskEntity(
            id = 1,
            parentListLocalId = 1,
            title = "task",
            dueDate = now,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now - 1.days,
            position = "09999999999999999999",
        )
        val completedTaskLast = TaskEntity(
            id = 2,
            parentListLocalId = 1,
            title = "task",
            dueDate = null,
            lastUpdateDate = now,
            isCompleted = true,
            completionDate = now,
            position = "09999999999999999998",
        )

        val sorted = sortCompletedTasks(
            listOf(
                completedTaskLast,
                completedTaskFirst,
            )
        )

        assertEquals(2, sorted.size)
        assertEquals(completedTaskLast.id, sorted[0].id)
        assertEquals(completedTaskFirst.id, sorted[1].id)
    }
}