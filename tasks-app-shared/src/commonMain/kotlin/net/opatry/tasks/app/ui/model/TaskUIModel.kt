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

package net.opatry.tasks.app.ui.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

sealed class DateRange {
    data object None : DateRange()
    data class Overdue(val date: LocalDate, val numberOfDays: Int) : DateRange()
    data object Yesterday : DateRange()
    data object Today : DateRange()
    data object Tomorrow : DateRange()
    data class Later(val date: LocalDate, val numberOfDays: Int) : DateRange()
}

data class TaskUIModel(
    val id: Long,
    val title: String,
    val dueDate: LocalDate? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val position: String = "", // FIXME for debug?
    val indent: Int = 0,
) {
    val dateRange: DateRange
        get() {
            val now = Clock.System.now()

            // get number of weeks between two dates
            val todayLocalDate = now.toLocalDateTime(TimeZone.UTC).date
            val dueLocalDate = dueDate ?: return DateRange.None
            val daysUntilDueDate = todayLocalDate.daysUntil(dueLocalDate)

            return when {
                daysUntilDueDate < -1 -> DateRange.Overdue(dueLocalDate, -daysUntilDueDate)
                daysUntilDueDate == -1 -> DateRange.Yesterday
                daysUntilDueDate == 0 -> DateRange.Today
                daysUntilDueDate == 1 -> DateRange.Tomorrow
                // daysUntilDueDate > 1
                else -> DateRange.Later(dueLocalDate, daysUntilDueDate)
            }
        }

    val canUnindent: Boolean = indent > 0
    val canIndent: Boolean = indent < 1
    val canCreateSubTask: Boolean = indent == 0
}
