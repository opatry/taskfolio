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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.TaskListRow
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.LABEL
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.REMAINING_TASKS_COUNT_BADGE
import net.opatry.tasks.app.ui.component.TaskListRowTestTag.REMAINING_TASKS_COUNT_BADGE_LABEL
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TaskListRowTest {

    @Test
    fun `when task list with title then title should be displayed`() = runComposeUiTest {
        val taskList = createTaskList("Hello World!")
        setContent {
            TaskListRow(taskList) {}
        }

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(LABEL, useUnmergedTree = true)
            .assertTextContains("Hello World!")
    }

    @Test
    fun `when task list is clicked then onClick callback should be triggered`() = runComposeUiTest {
        val taskList = createTaskList()
        var clicked = false
        setContent {
            TaskListRow(taskList, Modifier.testTag("ROW")) {
                clicked = true
            }
        }

        onNodeWithTag("ROW")
            .assertIsEnabled()
            .performClick()

        assertTrue(clicked, "onClick callback should have been triggered")
    }

    @Test
    fun `when task list has remaining tasks and is selected then badge counter should be hidden`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0)
        setContent {
            TaskListRow(taskList, isSelected = true) {}
        }

        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `when task list has no remaining tasks then badge counter should be hidden`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0)
        setContent {
            TaskListRow(taskList) {}
        }

        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `when task list has remaining tasks then badge counter should be displayed with count`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 14)
        setContent {
            TaskListRow(taskList) {}
        }

        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE, useUnmergedTree = true)
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE_LABEL, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains(14.toString())
    }

    @Test
    fun `when task list has more than 999 remaining tasks then badge counter should be displayed with 999+ label`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1500)
        setContent {
            TaskListRow(taskList) {}
        }

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE, useUnmergedTree = true)
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(REMAINING_TASKS_COUNT_BADGE_LABEL, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("999+")
    }
}