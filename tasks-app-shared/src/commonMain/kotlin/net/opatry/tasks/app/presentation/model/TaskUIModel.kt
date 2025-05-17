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

package net.opatry.tasks.app.presentation.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import net.opatry.tasks.data.toTaskPosition
import net.opatry.tasks.domain.TaskId
import java.math.BigInteger

sealed class DateRange {
    open val date: LocalDate? = null
    open val numberOfDays: Int? = null
    data object None : DateRange()
    data class Overdue(override val date: LocalDate, override val numberOfDays: Int) : DateRange()
    data class Today(override val date: LocalDate) : DateRange() {
        override val numberOfDays: Int = 0
    }
    data class Later(override val date: LocalDate, override val numberOfDays: Int) : DateRange()
}

operator fun DateRange.compareTo(other: DateRange): Int {
    // local variable for non null smart cast convenience
    val lhsNumberOfDays = this.numberOfDays
    val rhsNumberOfDays = other.numberOfDays
    // No date should come last
    return when {
        lhsNumberOfDays == null -> 1
        rhsNumberOfDays == null -> -1
        else -> lhsNumberOfDays.compareTo(rhsNumberOfDays)
    }
}

sealed interface TaskUIModel {
    val id: TaskId
    val title: String
    val notes: String
    val position: String
    val dueDate: LocalDate?

    val dateRange: DateRange
        get() {
            val now = Clock.System.now()

            // get number of weeks between two dates
            val todayLocalDate = now.toLocalDateTime(TimeZone.UTC).date
            val dueLocalDate = dueDate ?: return DateRange.None
            val daysUntilDueDate = todayLocalDate.daysUntil(dueLocalDate)

            return when {
                daysUntilDueDate < 0 -> DateRange.Overdue(dueLocalDate, daysUntilDueDate)
                daysUntilDueDate > 0 -> DateRange.Later(dueLocalDate, daysUntilDueDate)
                else -> DateRange.Today(dueLocalDate)
            }
        }

    data class Todo(
        override val id: TaskId,
        override val title: String,
        override val dueDate: LocalDate? = null,
        override val notes: String = "",
        override val position: String = 0.toTaskPosition(),
        val indent: Int = 0,
        val canMoveToTop: Boolean = false,
        val canUnindent: Boolean = false,
        val canIndent: Boolean = false,
        val canCreateSubTask: Boolean = false,
    ) : TaskUIModel

    data class Done(
        override val id: TaskId,
        override val title: String,
        override val notes: String = "",
        override val position: String = BigInteger("9999999999999999999").toTaskPosition(),
        override val dueDate: LocalDate? = null,
        val completionDate: LocalDate,
    ) : TaskUIModel
}
