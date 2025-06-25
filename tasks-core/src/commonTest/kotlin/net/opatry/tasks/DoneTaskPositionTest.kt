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

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DoneTaskPositionTest {

    @Test
    fun `given completion date when fromCompletionDate then should create position with correct format`() {
        // Given
        val completionDate = Instant.fromEpochMilliseconds(1000)

        // When
        val position = DoneTaskPosition.fromCompletionDate(completionDate)

        // Then
        assertEquals(20, position.value.length)
        assertTrue(position.value.all(Char::isDigit))
        assertEquals("09999999999999998999", position.value)
    }

    @Test
    fun `given zero timestamp when fromCompletionDate then should handle zero timestamp correctly`() {
        // Given
        val completionDate = Instant.fromEpochMilliseconds(0)

        // When
        val position = DoneTaskPosition.fromCompletionDate(completionDate)

        // Then
        assertEquals("09999999999999999999", position.value)
    }

    @Test
    fun `given large timestamp when fromCompletionDate then should handle large timestamp correctly`() {
        // Given
        val completionDate = Instant.fromEpochMilliseconds(9999999999999999L)

        // When
        val position = DoneTaskPosition.fromCompletionDate(completionDate)

        // Then
        assertEquals("09990000000000000000", position.value)
    }

    @Test
    fun `given valid position string when fromPosition then should create position correctly`() {
        // Given
        val positionString = "09999999999999998999"

        // When
        val position = DoneTaskPosition.fromPosition(positionString)

        // Then
        assertEquals(positionString, position.value)
    }

    @Test
    fun `given non-numeric string when fromPosition then should throw IllegalArgumentException`() {
        // Given
        val nonNumericString = "abcd999999999999999"

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            DoneTaskPosition.fromPosition(nonNumericString)
        }
    }

    @Test
    fun `given different completion dates when compareTo then should compare in reverse chronological order`() {
        // Given
        val earlier = Instant.fromEpochMilliseconds(1000)
        val later = Instant.fromEpochMilliseconds(2000)
        val pos1 = DoneTaskPosition.fromCompletionDate(earlier)
        val pos2 = DoneTaskPosition.fromCompletionDate(later)

        // When & Then
        assertTrue(pos1 > pos2)
        assertTrue(pos2 < pos1)
    }

    @Test
    fun `given same completion date when compareTo then should be equal`() {
        // Given
        val completionDate = Instant.fromEpochMilliseconds(1000)
        val pos1 = DoneTaskPosition.fromCompletionDate(completionDate)
        val pos2 = DoneTaskPosition.fromCompletionDate(completionDate)

        // When & Then
        assertEquals(0, pos1.compareTo(pos2))
    }

    @Test
    fun `given DoneTaskPositions with same completion date when equals and hashCode then should be equal with same hashCode`() {
        // Given
        val completionDate = Instant.fromEpochMilliseconds(1000)
        val pos1 = DoneTaskPosition.fromCompletionDate(completionDate)
        val pos2 = DoneTaskPosition.fromCompletionDate(completionDate)
        val pos3 = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(2000))

        // When & Then
        assertEquals(pos1, pos2)
        assertNotEquals(pos1, pos3)
        assertEquals(pos1.hashCode(), pos2.hashCode())
        assertNotEquals(pos1.hashCode(), pos3.hashCode())
    }

    @Test
    fun `given DoneTaskPosition when toString then should return meaningful representation`() {
        // Given
        val position = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000))

        // When
        val result = position.toString()

        // Then
        assertEquals(20, result.length)
        assertEquals("09999999999999998999", result)
    }

    @Test
    fun `given DoneTaskPosition and invalid TaskPosition type when compareTo then should throw IllegalArgumentException`() {
        // Given
        val donePos = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000))
        val invalidTaskPosition = object : TaskPosition {
            override val value: String = "12345678901234567890"
            override fun compareTo(other: TaskPosition): Int = 0
        }

        // When & Then
        assertFailsWith<IllegalArgumentException> {
            donePos.compareTo(invalidTaskPosition)
        }
    }

    @Test
    fun `given DoneTaskPosition and invalid object type when equals then should return false`() {
        // Given
        val donePos = DoneTaskPosition.fromCompletionDate(Instant.fromEpochMilliseconds(1000))
        val invalidObject = "not a TaskPosition"

        // When
        val result = donePos.equals(invalidObject)

        // Then
        assertFalse(result)
    }
}
