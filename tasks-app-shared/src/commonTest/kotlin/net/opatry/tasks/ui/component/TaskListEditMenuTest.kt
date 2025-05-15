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
import net.opatry.tasks.app.ui.component.TaskListEditMenu
import net.opatry.tasks.app.ui.component.TaskListEditMenuAction
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.CLEAR_COMPLETED_TASKS
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.EDIT_MENU
import net.opatry.tasks.app.ui.component.TaskListEditMenuTestTag.RENAME
import net.opatry.tasks.ui.screen.createTaskList
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TaskListEditMenuTest {
    @Test
    fun `when not expanded then EDIT_MENU should not be displayed`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = false,
                onDismiss = {},
                onAction = {},
            )
        }

        onNodeWithTag(EDIT_MENU)
            .assertDoesNotExist()
    }

    @Test
    fun `when expanded then EDIT_MENU should be displayed with all edit actions items`() = runComposeUiTest {
        val taskList = createTaskList()
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {},
            )
        }

        onNodeWithTag(EDIT_MENU)
            .assertIsDisplayed()

        onNodeWithTag(RENAME)
            .assertIsDisplayed()

        onNodeWithTag(CLEAR_COMPLETED_TASKS)
            .assertIsDisplayed()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
    }

    @Test
    fun `when dismissed then onDismiss callback should be fired`() = runComposeUiTest {
        val taskList = createTaskList()
        var dismissed = false
        setContent {
            Column {
                Text("Dismiss")
                TaskListEditMenu(
                    taskList = taskList,
                    expanded = true,
                    onDismiss = {
                        dismissed = true
                    },
                    onAction = {},
                )
            }
        }

        onNodeWithText("Dismiss")
            .performClick()

        assertTrue(dismissed)
    }

    @Test
    fun `when no completed tasks then CLEAR_COMPLETED_TASKS should be disabled`() = runComposeUiTest {
        val taskList = createTaskList(completedTaskCount = 0)
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {},
            )
        }

        onNodeWithTag(RENAME)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(CLEAR_COMPLETED_TASKS)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when some completed tasks then CLEAR_COMPLETED_TASKS should be enabled`() = runComposeUiTest {
        val taskList = createTaskList(completedTaskCount = 0)
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {},
            )
        }

        onNodeWithTag(RENAME)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(CLEAR_COMPLETED_TASKS)
            .assertIsDisplayed()
            .assertIsNotEnabled()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when RENAME is clicked then should notify Rename action`() = runComposeUiTest {
        val taskList = createTaskList()
        var action: TaskListEditMenuAction? = null
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {
                    action = it
                },
            )
        }

        onNodeWithTag(RENAME)
            .performClick()

        assertEquals(TaskListEditMenuAction.Rename, action)
    }

    @Test
    fun `when CLEAR_COMPLETED_TASKS is clicked then should notify ClearCompletedTasks action`() = runComposeUiTest {
        val taskList = createTaskList(completedTaskCount = 1)
        var action: TaskListEditMenuAction? = null
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {
                    action = it
                },
            )
        }

        onNodeWithTag(CLEAR_COMPLETED_TASKS)
            .performClick()

        assertEquals(TaskListEditMenuAction.ClearCompletedTasks, action)
    }

    @Test
    fun `when DELETE is clicked then should notify Delete action`() = runComposeUiTest {
        val taskList = createTaskList()
        var action: TaskListEditMenuAction? = null
        setContent {
            TaskListEditMenu(
                taskList = taskList,
                expanded = true,
                onDismiss = {},
                onAction = {
                    action = it
                },
            )
        }

        onNodeWithTag(DELETE)
            .performClick()

        assertEquals(TaskListEditMenuAction.Delete, action)
    }
}