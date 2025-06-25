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
import java.math.BigInteger

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TodoTaskPosition private constructor(internal val rawValue: BigInteger) : TaskPosition {
    actual override val value: String
        get() = rawValue.toString().padStart(20, '0')

    actual companion object {
        actual fun fromIndex(index: Int): TodoTaskPosition {
            return TodoTaskPosition(BigInteger.valueOf(index.toLong()))
        }

        actual fun fromPosition(position: String): TodoTaskPosition {
            return TodoTaskPosition(BigInteger(position))
        }
    }

    actual override fun compareTo(other: TaskPosition): Int {
        return when (other) {
            is TodoTaskPosition -> rawValue.compareTo(other.rawValue)
            is DoneTaskPosition -> rawValue.compareTo(other.rawValue)
            else -> throw IllegalArgumentException("Only TodoTaskPosition and DoneTaskPosition are supported")
        }
    }

    actual override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TodoTaskPosition) return false
        return rawValue == other.rawValue
    }

    actual override fun toString(): String = value
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DoneTaskPosition private constructor(internal val rawValue: BigInteger) : TaskPosition {
    actual companion object {
        private val UpperBound = BigInteger("9999999999999999999")
        actual fun fromCompletionDate(completionDate: Instant): DoneTaskPosition {
            return DoneTaskPosition(UpperBound - completionDate.toEpochMilliseconds().toBigInteger())
        }

        actual fun fromPosition(position: String): DoneTaskPosition {
            return DoneTaskPosition(BigInteger(position))
        }
    }

    actual override val value: String
        get() = rawValue.toString().padStart(20, '0')

    actual override fun compareTo(other: TaskPosition): Int {
        return when (other) {
            is TodoTaskPosition -> rawValue.compareTo(other.rawValue)
            is DoneTaskPosition -> rawValue.compareTo(other.rawValue)
            else -> throw IllegalArgumentException("Only TodoTaskPosition and DoneTaskPosition are supported")
        }
    }

    actual override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DoneTaskPosition) return false
        return rawValue == other.rawValue
    }

    actual override fun toString(): String = value
}
