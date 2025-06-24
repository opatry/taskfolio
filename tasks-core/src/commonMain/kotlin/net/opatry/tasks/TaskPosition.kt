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

interface TaskPosition : Comparable<TaskPosition> {
    val value: String
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class TodoTaskPosition : TaskPosition {
    companion object {
        fun fromIndex(index: Int): TodoTaskPosition
        fun fromPosition(position: String): TodoTaskPosition
    }

    override val value: String
    override fun compareTo(other: TaskPosition): Int
    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
    override fun toString(): String
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DoneTaskPosition : TaskPosition {
    companion object {
        fun fromCompletionDate(completionDate: Instant): DoneTaskPosition
        fun fromPosition(position: String): DoneTaskPosition
    }

    override val value: String
    override fun compareTo(other: TaskPosition): Int
    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
    override fun toString(): String
}
