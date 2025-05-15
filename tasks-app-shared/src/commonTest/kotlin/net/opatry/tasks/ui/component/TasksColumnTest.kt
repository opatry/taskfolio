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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ROW
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.REMAINING_TASK_ROW
import net.opatry.tasks.app.ui.component.TasksColumn
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.ALL_COMPLETE_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE_LABEL
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.FULLY_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.TASKS_COLUMN
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_pane_completed_section_title_with_count
import net.opatry.tasks.ui.screen.createTaskList
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test

@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class TasksColumnTest {
    @Test
    fun TasksColumn_TaskRows() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 2, completedTaskCount = 1)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                taskList = taskList,
            )
        }

        onAllNodesWithTag(REMAINING_TASK_ROW)
            .assertCountEquals(2)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
    }

    @Test
    fun TasksColumn_FullyEmptyState() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0, completedTaskCount = 0)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                taskList = taskList,
            )
        }

        onNodeWithTag(FULLY_EMPTY_STATE)
            .assertIsDisplayed()

        onNodeWithTag(TASKS_COLUMN)
            .assertDoesNotExist()
    }

    @Test
    fun TasksColumn_AllCompletedEmptyState() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0, completedTaskCount = 1)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                taskList = taskList,
            )
        }

        onNodeWithTag(FULLY_EMPTY_STATE)
            .assertDoesNotExist()

        onNodeWithTag(ALL_COMPLETE_EMPTY_STATE)
            .assertIsDisplayed()

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
    }

    @Test
    fun TasksColumn_NoCompletedSectionIfNotNeeded() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 2, completedTaskCount = 0)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                taskList = taskList,
            )
        }

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertDoesNotExist()

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)
    }

    @Test
    fun TasksColumn_ToggleCompletedSection() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1, completedTaskCount = 2)
        lateinit var completedTaskLabel: String
        setContent {
            completedTaskLabel = stringResource(Res.string.task_list_pane_completed_section_title_with_count, 2)
            TasksColumn(
                taskLists = listOf(taskList),
                taskList = taskList,
            )
        }

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
            .performClick()
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(COMPLETED_TASKS_TOGGLE_LABEL, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals(completedTaskLabel)

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(2)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .performClick()
            .assertIsDisplayed()

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)
    }
}