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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.TaskListEditMenuAction
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.EDIT_MENU
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_MENU
import net.opatry.tasks.app.ui.component.TaskListSortMenuTestTag.SORT_TITLE
import net.opatry.tasks.app.ui.component.TaskListTopAppBar
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.MORE_MENU_ICON
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.SORT_MENU_ICON
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.ui.screen.createTaskList
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TaskListTopAppBarTest {

    @Test
    fun `when task list has Toto as title then Toto should be displayed`() = runComposeUiTest {
        val taskList = createTaskList(title = "Toto")
        setContent {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = {},
                onEdit = {},
            )
        }

        onNodeWithText("Toto")
            .assertIsDisplayed()
    }

    @Test
    fun `when SORT_MENU_ICON is clicked then SORT_MENU should be displayed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = {},
                onEdit = {},
            )
        }

        onNodeWithText("Task List")
            .assertIsDisplayed()

        onNodeWithTag(SORT_MENU)
            .assertDoesNotExist()

        onNodeWithTag(SORT_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(SORT_MENU)
            .assertIsDisplayed()
    }

    @Test
    fun `when clicking outside of the menu then SORT_MENU menu should be closed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            Column {
                Text("Outside")
                TaskListTopAppBar(
                    taskList = taskList,
                    onSort = {},
                    onEdit = {},
                )
            }
        }

        onNodeWithTag(SORT_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithText("Outside")
            .performClick()

        onNodeWithTag(SORT_MENU)
            .assertDoesNotExist()
    }

    @Test
    fun `when clicking a sort item then corresponding sorting should be notified`() = runComposeUiTest {
        val taskList = createTaskList()
        var sorting: TaskListSorting? = null
        setContent {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = {
                    sorting = it
                },
                onEdit = {},
            )
        }

        onNodeWithTag(SORT_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(SORT_TITLE)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskListSorting.Title, sorting)
    }

    @Test
    fun `when MORE_MENU_ICON is clicked then EDIT_MENU should be displayed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = {},
                onEdit = {},
            )
        }

        onNodeWithText("Task List")
            .assertIsDisplayed()

        onNodeWithTag(EDIT_MENU)
            .assertDoesNotExist()

        onNodeWithTag(MORE_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(EDIT_MENU)
            .assertIsDisplayed()
    }

    @Test
    fun `when clicking outside of the menu then EDIT_MENU menu should be closed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            Column {
                Text("Outside")
                TaskListTopAppBar(
                    taskList = taskList,
                    onSort = {},
                    onEdit = {},
                )
            }
        }

        onNodeWithTag(MORE_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithText("Outside")
            .performClick()

        onNodeWithTag(EDIT_MENU)
            .assertDoesNotExist()
    }

    @Test
    fun `when clicking an action item then corresponding action should be notified`() = runComposeUiTest {
        val taskList = createTaskList()
        var action: TaskListEditMenuAction? = null
        setContent {
            TaskListTopAppBar(
                taskList = taskList,
                onSort = {},
                onEdit = {
                    action = it
                },
            )
        }

        onNodeWithTag(MORE_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskListEditMenuAction.Delete, action)
    }
}