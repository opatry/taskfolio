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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.TaskListSortMenu
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_DUE_DATE
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_MANUAL
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_MENU
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_TITLE
import net.opatry.tasks.data.TaskListSorting
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TaskListSortMenuTest {
    @Test
    fun `when not expanded then SORT_MENU should not be displayed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = false,
                onDismiss = {},
                onSortingClick = {},
            )
        }

        onNodeWithTag(SORT_MENU)
            .assertDoesNotExist()
    }

    @Test
    fun `when expanded then SORT_MENU should be displayed with all sorting actions items`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onSortingClick = {},
            )
        }

        onNodeWithTag(SORT_MENU)
            .assertIsDisplayed()

        onNodeWithTag(SORT_MANUAL)
            .assertIsDisplayed()

        onNodeWithTag(SORT_DUE_DATE)
            .assertIsDisplayed()

        onNodeWithTag(SORT_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun `when dismissed then onDismiss callback should be fired`() = runComposeUiTest {
        val taskList = createTaskList()
        var dismissed = false
        setContent {
            Column {
                Text("Dismiss")
                TaskListSortMenu(
                    taskList = taskList,
                    expanded = true,
                    onDismiss = {
                        dismissed = true
                    },
                    onSortingClick = {},
                )
            }
        }

        onNodeWithText("Dismiss")
            .performClick()

        assertTrue(dismissed)
    }

    @Test
    fun `when sorting is selected then corresponding menu item should be disabled`() = runComposeUiTest {
        val taskList = createTaskList().copy(sorting = TaskListSorting.Title)
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onSortingClick = {},
            )
        }

        onNodeWithTag(SORT_MANUAL)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(SORT_DUE_DATE)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(SORT_TITLE)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when SORT_MANUAL is clicked then should notify Manual sorting`() = runComposeUiTest {
        val taskList = createTaskList().copy(sorting = TaskListSorting.Title)
        var sorting: TaskListSorting? = null
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onSortingClick = {
                    sorting = it
                },
            )
        }

        onNodeWithTag(SORT_MANUAL)
            .performClick()

        assertEquals(TaskListSorting.Manual, sorting, "Click on SORT_MANUAL should trigger Manual sort action")
    }

    @Test
    fun `when SORT_DUE_DATE is clicked then should notify DueDate sorting`() = runComposeUiTest {
        val taskList = createTaskList().copy(sorting = TaskListSorting.Manual)
        var sorting: TaskListSorting? = null
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onSortingClick = {
                    sorting = it
                },
            )
        }

        onNodeWithTag(SORT_DUE_DATE)
            .performClick()

        assertEquals(TaskListSorting.DueDate, sorting)
    }

    @Test
    fun `when SORT_TITLE is clicked then should notify Title sorting`() = runComposeUiTest {
        val taskList = createTaskList().copy(sorting = TaskListSorting.DueDate)
        var sorting: TaskListSorting? = null
        setContent {
            TaskListSortMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onSortingClick = {
                    sorting = it
                },
            )
        }

        onNodeWithTag(SORT_TITLE)
            .performClick()

        assertEquals(TaskListSorting.Title, sorting)
    }
}