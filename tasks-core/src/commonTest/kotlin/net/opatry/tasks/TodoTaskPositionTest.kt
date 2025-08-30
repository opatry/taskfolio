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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TodoTaskPositionTest {

    @Test
    fun `given index 42 when fromIndex then should create position with correct 20-char zero-padded value`() {
        // Given
        val index = 42

        // When
        val position = TodoTaskPosition.fromIndex(index)

        // Then
        assertEquals(20, position.value.length)
        assertEquals("00000000000000000042", position.value)
    }

    @Test
    fun `given index 0 when fromIndex then should handle zero index correctly`() {
        // Given
        val index = 0

        // When
        val position = TodoTaskPosition.fromIndex(index)

        // Then
        assertEquals("00000000000000000000", position.value)
    }

    @Test
    fun `given large index when fromIndex then should handle large numbers correctly`() {
        // Given
        val index = 999999999

        // When
        val position = TodoTaskPosition.fromIndex(index)

        // Then
        assertEquals("00000000000999999999", position.value)
    }

    @Test
    fun `given valid position string when fromPosition then should create position correctly`() {
        // Given
        val positionString = "00000000000000000123"

        // When
        val position = TodoTaskPosition.fromPosition(positionString)

        // Then
        assertEquals(positionString, position.value)
    }

    @Test
    fun `given non-numeric string when fromPosition then should throw IllegalArgumentException`() {
        // Given
        val nonNumericString = "abcd000000000000000"

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            TodoTaskPosition.fromPosition(nonNumericString)
        }
    }

    @Test
    fun `given different TodoTaskPositions when compareTo then should compare correctly`() {
        // Given
        val pos1 = TodoTaskPosition.fromIndex(10)
        val pos2 = TodoTaskPosition.fromIndex(20)
        val pos3 = TodoTaskPosition.fromIndex(10)

        // When & Then
        assertTrue(pos1 < pos2)
        assertTrue(pos2 > pos1)
        assertEquals(0, pos1.compareTo(pos3))
    }

    @Test
    fun `given TodoTaskPosition and DoneTaskPosition when compareTo then TodoTaskPosition should be smaller`() {
        // Given
        val todoPos = TodoTaskPosition.fromIndex(100)
        val donePos = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000))

        // When & Then
        assertTrue(todoPos < donePos)
        assertTrue(donePos > todoPos)
    }

    @Test
    fun `given TodoTaskPositions with same index when equals and hashCode then should be equal with same hashCode`() {
        // Given
        val pos1 = TodoTaskPosition.fromIndex(42)
        val pos2 = TodoTaskPosition.fromIndex(42)
        val pos3 = TodoTaskPosition.fromIndex(43)

        // When & Then
        assertEquals(pos1, pos2)
        assertNotEquals(pos1, pos3)
        assertEquals(pos1.hashCode(), pos2.hashCode())
        assertNotEquals(pos1.hashCode(), pos3.hashCode())
    }

    @Test
    fun `given TodoTaskPosition when toString then should return meaningful representation`() {
        // Given
        val position = TodoTaskPosition.fromIndex(42)

        // When
        val result = position.toString()

        // Then
        assertEquals(20, result.length)
        assertEquals("00000000000000000042", result)
    }

    @Test
    fun `given TodoTaskPosition and invalid TaskPosition type when compareTo then should throw IllegalArgumentException`() {
        // Given
        val todoPos = TodoTaskPosition.fromIndex(42)
        val invalidTaskPosition = object : TaskPosition {
            override val value: String = "12345678901234567890"
            override fun compareTo(other: TaskPosition): Int = 0
        }

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            todoPos.compareTo(invalidTaskPosition)
        }
    }

    @Test
    fun `given TodoTaskPosition and invalid object type when equals then should return false`() {
        // Given
        val todoPos = TodoTaskPosition.fromIndex(42)
        val invalidObject = "not a TaskPosition"

        // When
        val result = todoPos.equals(invalidObject)

        // Then
        assertFalse(result)
    }
}
