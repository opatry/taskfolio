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

package net.opatry.tasks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TaskPositionTest {

    @Test
    fun `given TodoTaskPosition and DoneTaskPosition when compareTo then should maintain workflow order`() {
        // Given
        val todoPos = TodoTaskPosition.fromIndex(999999999)
        val donePos = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(0))

        // When & Then
        assertTrue(todoPos < donePos)
        assertTrue(donePos > todoPos)
    }

    @Test
    fun `given mixed task positions when sorted then should maintain task workflow order`() {
        // Given
        val todo1 = TodoTaskPosition.fromIndex(1)
        val todo2 = TodoTaskPosition.fromIndex(2)
        val done1 = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(2000)) // completed later
        val done2 = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000)) // completed earlier
        val positions = listOf(done1, todo2, done2, todo1)

        // When
        val sorted = positions.sorted()

        // Then
        assertEquals(listOf(todo1, todo2, done1, done2), sorted)
    }

    @Test
    fun `given task positions when accessing value property then should return correct 20-character string`() {
        // Given
        val todoPos = TodoTaskPosition.fromIndex(42)
        val donePos = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000))

        // When & Then
        assertEquals(20, todoPos.value.length)
        assertEquals(20, donePos.value.length)
        assertTrue(todoPos.value.all(Char::isDigit))
        assertTrue(donePos.value.all(Char::isDigit))
    }

    @Test
    fun `given task positions when roundtrip serialization then should recreate equal objects`() {
        // Given
        val originalTodo = TodoTaskPosition.fromIndex(123)
        val originalDone = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(5000))

        // When
        val recreatedTodo = TodoTaskPosition.fromPosition(originalTodo.value)
        val recreatedDone = DoneTaskPosition.fromPosition(originalDone.value)

        // Then
        assertEquals(originalTodo, recreatedTodo)
        assertEquals(originalDone, recreatedDone)
    }

    @Test
    fun `given boundary values when creating positions then should work correctly`() {
        // Given
        val maxIndex = Int.MAX_VALUE
        val maxTimestamp = 999999999999999999L

        // When
        val maxTodo = TodoTaskPosition.fromIndex(maxIndex)
        val maxDone = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(maxTimestamp))

        // Then
        assertEquals(20, maxTodo.value.length)
        assertEquals(20, maxDone.value.length)
        assertEquals("00000000002147483647", maxTodo.value)
        assertEquals("09000000000000000000", maxDone.value)
    }
}