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

package net.opatry.tasks.presentation.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.opatry.tasks.TodoTaskPosition
import net.opatry.tasks.app.presentation.asTaskUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.data.model.TaskDataModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun buildMoments(dateStr: String = "2024-10-16"): Pair<LocalDate, Instant> {
    val date = LocalDate.parse(dateStr)
    val instant = LocalDateTime.parse("${date}T00:00:00").toInstant(TimeZone.UTC)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertEquals(42L, taskUIModel.id.value)
        assertEquals("title", taskUIModel.title)
        assertEquals("notes", taskUIModel.notes)
        assertEquals(date, taskUIModel.dueDate)
        assertEquals(TodoTaskPosition.fromPosition("00000000000000000042"), taskUIModel.position)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertEquals(null, taskUIModel.dueDate)
    }

    @Test
    fun `no completion date should throw for completed task`() {
        val task = TaskDataModel(
            id = 42L,
            title = "title",
            isCompleted = true,
            completionDate = null,
            lastUpdateDate = Clock.System.now(),
            position = "09999999999999999999",
            indent = 0,
            isParentTask = false,
        )

        assertFailsWith<IllegalArgumentException> {
            task.asTaskUIModel()
        }
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Done>(taskUIModel)
        assertEquals(completionDate, taskUIModel.completionDate)
    }

    @Test
    fun `canMoveToTop when not first task and not indented should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertTrue(taskUIModel.canMoveToTop)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertFalse(taskUIModel.canMoveToTop)
    }

    @Test
    fun `canIndent when not already indented and not first task should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertFalse(taskUIModel.canIndent)
    }

    @Test
    fun `canIndent when a parent task should be false`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000001",
            indent = 0,
            isParentTask = true,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertFalse(taskUIModel.canIndent)
    }

    @Test
    fun `canUnindent when already indented should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 1,
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertFalse(taskUIModel.canUnindent)
    }

    @Test
    fun `canCreateSubTask for unindented task should be true`() {
        val task = TaskDataModel(
            id = 0L,
            title = "title",
            isCompleted = false,
            lastUpdateDate = Clock.System.now(),
            position = "00000000000000000000",
            indent = 0,
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
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
            isParentTask = false,
        )

        val taskUIModel = task.asTaskUIModel()

        assertIs<TaskUIModel.Todo>(taskUIModel)
        assertFalse(taskUIModel.canCreateSubTask)
    }
}