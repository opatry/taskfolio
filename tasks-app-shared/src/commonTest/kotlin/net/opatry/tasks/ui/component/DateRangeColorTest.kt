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

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import net.opatry.tasks.app.ui.component.toColor
import net.opatry.tasks.app.ui.model.DateRange
import net.opatry.tasks.ui.screen.today
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class DateRangeColorTest {
    @Test
    fun DateRangeColor_LastYear() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val date = today.minus(1, DateTimeUnit.YEAR)
            val dateRange = DateRange.Overdue(date, -365)
            expectedColor = MaterialTheme.colorScheme.error
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_TwoWeeksAgo() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val dateRange = DateRange.Overdue(today.minus(2, DateTimeUnit.WEEK), -14)
            expectedColor = MaterialTheme.colorScheme.error
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_TwoDaysAgo() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val dateRange = DateRange.Overdue(today.minus(2, DateTimeUnit.DAY), -2)
            expectedColor = MaterialTheme.colorScheme.error
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_Yesterday() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val dateRange = DateRange.Overdue(today.minus(1, DateTimeUnit.DAY), -1)
            expectedColor = MaterialTheme.colorScheme.error
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_Today() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val dateRange = DateRange.Today(today)
            expectedColor = MaterialTheme.colorScheme.primary
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_Tomorrow() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val dateRange = DateRange.Later(today.plus(1, DateTimeUnit.DAY), 1)
            expectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_InTwoDays() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val date = today.plus(2, DateTimeUnit.DAY)
            val dateRange = DateRange.Later(date, 2)
            expectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_InTwoWeeks() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val date = today.plus(2, DateTimeUnit.DAY)
            val dateRange = DateRange.Later(date, 2)
            expectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun DateRangeColor_NextYear() = runComposeUiTest {
        var expectedColor: Color? = null
        var actualColor: Color? = null
        setContent {
            val date = today.plus(1, DateTimeUnit.YEAR)
            val dateRange = DateRange.Later(date, 2)
            expectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            actualColor = dateRange.toColor()
        }

        assertEquals(expectedColor, actualColor)
    }
}