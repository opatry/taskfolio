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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.ui.component.TaskListsColumn
import net.opatry.tasks.app.ui.component.TaskListsPaneTestTag
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class TaskListsColumnTest {
    @Test
    fun TaskListsColumn_NewTaskListButton() = runComposeUiTest {
        var newTaskClickCount = 0
        setContent {
            TaskListsColumn(emptyList(), selectedItem = null, onNewTaskList = { ++newTaskClickCount }, onItemClick = {})
        }

        onNodeWithTag(TaskListsPaneTestTag.NEW_TASK_LIST_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, newTaskClickCount, "New task callback should have been called once")
    }

    @Test
    fun TaskListsColumn_TaskListSelection() = runComposeUiTest {
        val taskList = TaskListUIModel(TaskListId(1), "Task list 1")
        var selectedList: TaskListUIModel? = null
        setContent {
            TaskListsColumn(listOf(taskList), selectedItem = null, onNewTaskList = {}, onItemClick = { selectedList = it })
        }

        onAllNodesWithTag(TaskListsPaneTestTag.TASK_LIST_ROW)
            .assertCountEquals(1)
            .onFirst()
            .performClick()

        assertEquals(taskList, selectedList, "Task click callback should have been called")
    }

    @Test
    fun TaskListsColumn_SelectedTaskList() = runComposeUiTest {
        val taskList1 = TaskListUIModel(TaskListId(1), "Task list 1")
        val taskList2 = TaskListUIModel(TaskListId(2), "Task list 2")
        setContent {
            TaskListsColumn(listOf(taskList1, taskList2), selectedItem = taskList1, onNewTaskList = {}, onItemClick = {})
        }

        onAllNodesWithTag(TaskListsPaneTestTag.TASK_LIST_ROW)
            .assertCountEquals(2)
            .onFirst()
            .assertIsSelected()

        onAllNodesWithTag(TaskListsPaneTestTag.TASK_LIST_ROW)
            .onLast()
            .assertIsNotSelected()
    }
}