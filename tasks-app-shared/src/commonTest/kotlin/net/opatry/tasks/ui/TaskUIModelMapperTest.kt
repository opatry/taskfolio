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

package net.opatry.tasks.ui

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.opatry.tasks.app.ui.asTaskUIModel
import net.opatry.tasks.data.model.TaskDataModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun buildMoments(dateStr: String = "2024-10-16"): Pair<LocalDate, Instant> {
    val date = LocalDate.parse(dateStr)
    val instant = LocalDateTime.parse("${date}T00:00:00").toInstant(TimeZone.currentSystemDefault())
    return date to instant
}

class TaskUIModelMapperTest {

    @Test
    fun `basic properties should be mapped`() {
        val (date, instant) = buildMoments()

        val task = TaskDataModel(
            id = 42L,
            title = "title",
            notes = "notes",
            isCompleted = false,
            dueDate = instant,
            lastUpdateDate = instant,
            completionDate = null,
            position = "00000000000000000042",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertEquals(42L, taskUIModel.id.value)
        assertEquals("title", taskUIModel.title)
        assertEquals("notes", taskUIModel.notes)
        assertEquals(false, taskUIModel.isCompleted)
        assertEquals(date, taskUIModel.dueDate)
        assertEquals("00000000000000000042", taskUIModel.position)
        assertEquals(1, taskUIModel.indent)
    }

    @Test
    fun `no due date is mapped to null`() {
        val task = TaskDataModel(
            id = 42L,
            title = "title",
            dueDate = null,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertEquals(null, taskUIModel.dueDate)
    }

    @Test
    fun `no completion date is mapped to null`() {
        val task = TaskDataModel(
            id = 42L,
            title = "title",
            completionDate = null,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertEquals(null, taskUIModel.completionDate)
    }

    @Test
    fun `completion date is mapped to LocalDate`() {
        val (completionDate, instant) = buildMoments()
        val task = TaskDataModel(
            id = 42L,
            title = "title",
            isCompleted = true,
            completionDate = instant,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.isCompleted)
        assertEquals(completionDate, taskUIModel.completionDate)
    }

    @Test
    fun `canMoveToTop when not completed, not first task and not indented should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.canMoveToTop)
    }

    @Test
    fun `canMoveToTop when completed should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = true,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canMoveToTop)
    }

    @Test
    fun `canMoveToTop when first task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canMoveToTop)
    }

    @Test
    fun `canMoveToTop when indented task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canMoveToTop)
    }

    @Test
    fun `canIndent when not completed, not already indented and not first task should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.canIndent)
    }

    @Test
    fun `canIndent when first task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canIndent)
    }

    @Test
    fun `canIndent when already indented should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canIndent)
    }

    @Test
    fun `canIndent when completed should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = true,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canIndent)
    }

    @Test
    fun `canUnindent when not completed, already indented should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.canUnindent)
    }

    @Test
    fun `canUnindent when first subtask should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.canUnindent)
    }

    @Test
    fun `canUnindent when not indented should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canUnindent)
    }

    @Test
    fun `canUnindent when completed should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = true,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canUnindent)
    }

    @Test
    fun `canCreateSubTask for not completed and unindented task should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertTrue(taskUIModel.canCreateSubTask)
    }

    @Test
    fun `canCreateSubTask for indented task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 1,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canCreateSubTask)
    }

    @Test
    fun `canCreateSubTask for completed task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = true,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
        )

        val taskUIModel = task.asTaskUIModel()

        assertFalse(taskUIModel.canCreateSubTask)
    }
}