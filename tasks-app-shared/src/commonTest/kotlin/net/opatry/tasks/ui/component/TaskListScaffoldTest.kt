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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskListScaffold
import net.opatry.tasks.app.ui.component.TaskListScaffoldTestTag.ADD_TASK_FAB
import kotlin.test.Test
import kotlin.test.assertTrue
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.MORE_MENU_ICON as TASK_LIST_MORE_MENU_ICON
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.SORT_MENU_ICON as TASK_LIST_SORT_MENU_ICON
import net.opatry.tasks.app.ui.component.TaskListTopAppBarTestTag.TITLE as TASK_LIST_PANE_TITLE

@OptIn(ExperimentalTestApi::class)
class TaskListScaffoldTest {
    @Test
    fun `when list is broken then menu should be hidden`() = runComposeUiTest {
        val brokenList = TaskListUIModel(
            id = TaskListId(value = 1),
            title = "broken list",
            remainingTasks = mapOf(
                null to listOf(
                    TaskUIModel.Todo(
                        id = TaskId(1),
                        title = "broken indent",
                        indent = 42,
                    )
                )
            )
        )
        setContent {
            TaskListScaffold(emptyList(), brokenList)
        }

        onNodeWithTag(TASK_LIST_PANE_TITLE)
            .assertIsDisplayed()
            .assertTextEquals("broken list")

        onNodeWithTag(TASK_LIST_SORT_MENU_ICON)
            .assertDoesNotExist()

        onNodeWithTag(TASK_LIST_MORE_MENU_ICON)
            .assertDoesNotExist()
    }

    @Test
    fun `when list is broken then add task FAB should be hidden`() = runComposeUiTest {
        val brokenList = TaskListUIModel(
            id = TaskListId(value = 1),
            title = "broken list",
            remainingTasks = mapOf(
                null to listOf(
                    TaskUIModel.Todo(
                        id = TaskId(1),
                        title = "broken indent",
                        indent = 42,
                    )
                )
            )
        )
        setContent {
            TaskListScaffold(emptyList(), brokenList)
        }

        onNodeWithTag(ADD_TASK_FAB)
            .assertDoesNotExist()
    }

    @Test
    fun `when add task FAB is clicked then add task action should be triggered`() = runComposeUiTest {
        val list = createTaskList()
        var addTaskClicked = false
        setContent {
            TaskListScaffold(
                taskLists = emptyList(),
                taskList = list,
                onNewTask = {
                    addTaskClicked = true
                }
            )
        }

        onNodeWithTag(ADD_TASK_FAB)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertTrue(addTaskClicked, "Add task action was not triggered")
    }
}