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

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.opatry.tasks.data.entity.TaskEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskSortingTest {

    @Test
    fun `minimum completed date sorting position`() {
        // minimum timestamp sorting value (1970-01-01T00:00:00Z)
        val tMin = LocalDateTime(1970, 1, 1, 0, 0, 0).toInstant(TimeZone.UTC)
        assertEquals("09999999999999999999", tMin.asCompletedTaskPosition())
    }

    @Test
    fun `arbitrary completed date sorting position`() {
        // an arbitrary timestamp (2024-10-29T15:54:12Z)
        val t = LocalDateTime(2024, 10, 29, 15, 54, 12).toInstant(TimeZone.UTC)
        assertEquals("09999998269782747999", t.asCompletedTaskPosition())
    }

    @Test
    fun `maximum completed date sorting position`() {
        // RFC 3339 timestamp supposed maximum value (9999-12-31T23:59:59Z)
        val tMax = LocalDateTime(9999, 12, 31, 23, 59, 59).toInstant(TimeZone.UTC)
        assertEquals("09999746597699200999", tMax.asCompletedTaskPosition())
    }

    @Test
    fun `last completed task is sorted first`() {
        val completedTasks = listOf(
            TaskEntity(
                id = 1,
                title = "t1",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = true,
                completionDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                position = ""
            ),
            TaskEntity(
                id = 2,
                title = "t2",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 29, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = true,
                completionDate = LocalDateTime(2024, 10, 29, 12, 0, 0).toInstant(TimeZone.UTC),
                position = ""
            ),
        )

        val sortedTasks = computeTaskPositions(completedTasks)

        assertEquals(completedTasks.size, sortedTasks.size)
        assertEquals(2, sortedTasks[0].id)
        assertEquals("09999998269796799999", sortedTasks[0].position)
        assertEquals(1, sortedTasks[1].id)
        assertEquals("09999998269883199999", sortedTasks[1].position)
    }

    @Test
    fun `completed tasks comes after others`() {
        val tasks = listOf(
            TaskEntity(
                id = 0,
                title = "t1",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 1,
                title = "t2",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 2,
                title = "t3",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = true,
                completionDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                position = ""
            ),
            TaskEntity(
                id = 3,
                title = "t4",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 29, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = true,
                completionDate = LocalDateTime(2024, 10, 29, 12, 0, 0).toInstant(TimeZone.UTC),
                position = ""
            ),
        )

        val sortedTasks = computeTaskPositions(tasks)

        assertEquals(tasks.size, sortedTasks.size)
        assertEquals(0, sortedTasks[0].id)
        assertEquals("00000000000000000000", sortedTasks[0].position)
        assertEquals(1, sortedTasks[1].id)
        assertEquals("00000000000000000001", sortedTasks[1].position)
        assertEquals(tasks.size, sortedTasks.size)
        assertEquals(3, sortedTasks[2].id)
        assertEquals("09999998269796799999", sortedTasks[2].position)
        assertEquals(2, sortedTasks[3].id)
        assertEquals("09999998269883199999", sortedTasks[3].position)
    }

    @Test
    fun `sorting of tasks is clustered by list`() {
        val tasks = listOf(
            TaskEntity(
                id = 0,
                title = "t1",
                parentListLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 1,
                title = "t2",
                parentListLocalId = 1,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 2,
                title = "t3",
                parentListLocalId = 1,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
        )

        val sortedTasks = computeTaskPositions(tasks)

        // position is reset for each list
        assertEquals(tasks.size, sortedTasks.size)
        assertEquals(0, sortedTasks[0].id)
        assertEquals("00000000000000000000", sortedTasks[0].position)
        assertEquals(1, sortedTasks[1].id)
        assertEquals("00000000000000000000", sortedTasks[1].position)
        assertEquals(2, sortedTasks[2].id)
        assertEquals("00000000000000000001", sortedTasks[2].position)
    }

    @Test
    fun `sorting of tasks is clustered by parent task`() {
        val tasks = listOf(
            TaskEntity(
                id = 0,
                title = "t1",
                parentListLocalId = 0,
                parentTaskLocalId = 0,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 1,
                title = "t2",
                parentListLocalId = 0,
                parentTaskLocalId = 1,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
            TaskEntity(
                id = 2,
                title = "t3",
                parentListLocalId = 0,
                parentTaskLocalId = 1,
                lastUpdateDate = LocalDateTime(2024, 10, 28, 12, 0, 0).toInstant(TimeZone.UTC),
                isCompleted = false,
                completionDate = null,
                position = ""
            ),
        )

        val sortedTasks = computeTaskPositions(tasks)

        // position is reset for each parent task
        assertEquals(tasks.size, sortedTasks.size)
        assertEquals(0, sortedTasks[0].id)
        assertEquals("00000000000000000000", sortedTasks[0].position)
        assertEquals(1, sortedTasks[1].id)
        assertEquals("00000000000000000000", sortedTasks[1].position)
        assertEquals(2, sortedTasks[2].id)
        assertEquals("00000000000000000001", sortedTasks[2].position)
    }
}