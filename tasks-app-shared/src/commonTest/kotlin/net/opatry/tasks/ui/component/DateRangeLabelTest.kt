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

package net.opatry.tasks.ui.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import net.opatry.tasks.app.ui.component.toLabel
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_days_ago
import net.opatry.tasks.resources.task_due_date_label_today
import net.opatry.tasks.resources.task_due_date_label_tomorrow
import net.opatry.tasks.resources.task_due_date_label_weeks_ago
import net.opatry.tasks.resources.task_due_date_label_yesterday
import net.opatry.tasks.ui.screen.today
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class DateRangeLabelTest {
    @Test
    fun DueDateLabel_LastYear() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val date = today.minus(1, DateTimeUnit.YEAR)
            val dateRange = DateRange.Overdue(date, -365)
            expectedLabel = pluralStringResource(Res.plurals.task_due_date_label_weeks_ago, 52, 52)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Test
    fun DueDateLabel_TwoWeeksAgo() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val dateRange = DateRange.Overdue(today.minus(2, DateTimeUnit.WEEK), -14)
            expectedLabel = pluralStringResource(Res.plurals.task_due_date_label_weeks_ago, 2, 2)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Test
    fun DueDateLabel_TwoDaysAgo() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val dateRange = DateRange.Overdue(today.minus(2, DateTimeUnit.DAY), -2)
            expectedLabel = pluralStringResource(Res.plurals.task_due_date_label_days_ago, 2, 2)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Test
    fun DueDateLabel_Yesterday() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val dateRange = DateRange.Overdue(today.minus(1, DateTimeUnit.DAY), -1)
            expectedLabel = stringResource(Res.string.task_due_date_label_yesterday)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Test
    fun DueDateLabel_Today() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val dateRange = DateRange.Today(today)
            expectedLabel = stringResource(Res.string.task_due_date_label_today)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Test
    fun DueDateLabel_Tomorrow() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val dateRange = DateRange.Later(today.plus(1, DateTimeUnit.DAY), 1)
            expectedLabel = stringResource(Res.string.task_due_date_label_tomorrow)
            actualLabel = dateRange.toLabel()
        }

        assertEquals(expectedLabel, actualLabel)
    }

    @Ignore("No proper date formatting with localization")
    @Test
    fun DueDateLabel_InTwoDays() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val date = today.plus(2, DateTimeUnit.DAY)
            val dateRange = DateRange.Later(date, 2)
            expectedLabel = "Mon, Oct 12" // TODO
            actualLabel = dateRange.toLabel()
        }

        // TODO ease checking date formatting taking into account localization
        // assertEquals(expectedLabel, actualLabel)
    }

    @Ignore("No proper date formatting with localization")
    @Test
    fun DueDateLabel_InTwoWeeks() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val date = today.plus(2, DateTimeUnit.DAY)
            val dateRange = DateRange.Later(date, 2)
            expectedLabel = "Mon, Oct 12" // TODO
            actualLabel = dateRange.toLabel()
        }

        // TODO ease checking date formatting taking into account localization
        // assertEquals(expectedLabel, actualLabel)
    }

    @Ignore("No proper date formatting with localization")
    @Test
    fun DueDateLabel_NextYear() = runComposeUiTest {
        lateinit var expectedLabel: String
        lateinit var actualLabel: String
        setContent {
            val date = today.plus(1, DateTimeUnit.YEAR)
            val dateRange = DateRange.Later(date, 2)
            expectedLabel = "October 12, 2025" // TODO
            actualLabel = dateRange.toLabel()
        }

        // TODO ease checking date formatting taking into account localization
        // assertEquals(expectedLabel, actualLabel)
    }
}