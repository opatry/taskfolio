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

package net.opatry.tasks.ui.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import net.opatry.tasks.app.ui.component.TaskAction
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.ADD_SUBTASK
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.INDENT
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_NEW_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_TOP
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.TASK_MENU
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.UNINDENT
import net.opatry.tasks.app.ui.screen.RemainingTaskRow
import net.opatry.tasks.app.ui.screen.TaskListPaneTestTag.REMAINING_TASK_DUE_DATE_CHIP
import net.opatry.tasks.app.ui.screen.TaskListPaneTestTag.REMAINING_TASK_ICON
import net.opatry.tasks.app.ui.screen.TaskListPaneTestTag.REMAINING_TASK_MENU_ICON
import net.opatry.tasks.app.ui.screen.TaskListPaneTestTag.REMAINING_TASK_NOTES
import net.opatry.tasks.app.ui.screen.TaskListPaneTestTag.REMAINING_TASK_ROW
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_weeks_ago
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class RemainingTaskRowTest {
    @Test
    fun RemainingTaskRow_Layout() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first().copy(
            title = "My Task",
            notes = "",
            dueDate = null
        )
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithText("My Task")
            .assertIsDisplayed()

        onNodeWithTag(REMAINING_TASK_NOTES)
            .assertDoesNotExist()

        onNodeWithTag(REMAINING_TASK_DUE_DATE_CHIP)
            .assertDoesNotExist()
    }

    @Test
    fun RemainingTaskRow_LayoutFull() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first().copy(
            title = "My Task With notes & date",
            notes = "My notes",
            dueDate = today.minus(2, DateTimeUnit.WEEK)
        )
        lateinit var twoWeeksAgo: String
        setContent {
            twoWeeksAgo = pluralStringResource(Res.plurals.task_due_date_label_weeks_ago, 2, 2)
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithText("My Task With notes & date")
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(REMAINING_TASK_NOTES, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("My notes")

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(REMAINING_TASK_DUE_DATE_CHIP, useUnmergedTree = true)
            .assertIsDisplayed()
            .onChildren()
            .assertCountEquals(1)
            .onFirst()
            .assertTextEquals(twoWeeksAgo)
    }

    @Test
    fun RemainingTaskRow_Complete() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_ICON)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.ToggleCompletion, action, "Toggle completion action should have been triggered")
    }

    @Test
    fun RemainingTaskRow_Menu() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(TASK_MENU)
            .assertDoesNotExist()

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(TASK_MENU)
            .assertIsDisplayed()
    }

    @Test
    fun RemainingTaskRow_Edit() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_ROW)
            .performClick()

        assertEquals(TaskAction.Edit, action, "Click on cell should trigger Edit action")
    }

    @Test
    fun RemainingTaskRow_EditDueDate() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_DUE_DATE_CHIP)
            .performClick()

        assertEquals(TaskAction.UpdateDueDate, action, "UpdateDueDate action should have been triggered")
    }

    // TODO
    //  AddSubTask depends if task is already a subtask or not, or if it's the first of the list
    @Test
    fun RemainingTaskRow_Menu_AddSubTask() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(ADD_SUBTASK)
            .assertIsDisplayed()
            // TODO not implemented yet
            .assertIsNotEnabled()

        // TODO restore once implemented
        //  assertEquals(TaskAction.AddSubTask, action, "AddSubTask action should have been triggered")
    }

    // TODO
    //  MoveToTop depends if task is the first of the list
    @Test
    fun RemainingTaskRow_Menu_MoveToTop() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(MOVE_TO_TOP)
            .assertDoesNotExist()

        // TODO restore once implemented
        //  assertEquals(TaskAction.MoveToTop, action, "MoveToTop action should have been triggered")
    }

    // TODO
    //  Unindent depends if task is already a subtask
    @Test
    fun RemainingTaskRow_Menu_Unindent() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(UNINDENT)
            .assertDoesNotExist()

        // TODO restore once implemented
        //  assertEquals(TaskAction.Unindent, action, "Unindent action should have been triggered")
    }

    // TODO
    //  Indent depends if task is already a subtask
    @Test
    fun RemainingTaskRow_Menu_Indent() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(INDENT)
            .assertIsDisplayed()
            // TODO not implemented yet
            .assertIsNotEnabled()

        // TODO restore once implemented
        //  assertEquals(TaskAction.Indent, action, "Indent action should have been triggered")
    }

    @Test
    fun RemainingTaskRow_Menu_MoveToNewList() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(MOVE_TO_NEW_LIST)
            .assertIsDisplayed()
            // TODO not implemented yet
            .assertIsNotEnabled()

        // TODO restore once implemented
        //  assertEquals(TaskAction.MoveToNewList, action, "MoveToNewList action should have been triggered")
    }

    // TODO
    //  Task parent list entry should be disabled, need several task list to test fully
    @Test
    fun RemainingTaskRow_Menu_MoveToList() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onAllNodesWithTag(MOVE_TO_LIST)
            // TODO not implemented yet
            .assertCountEquals(0)

        // TODO restore once implemented
        //  assertEquals(TaskAction.MoveToList(taskList2), action, "MoveToList action should have been triggered")
    }

    @Test
    fun RemainingTaskRow_Menu_Delete() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.Delete, action, "Delete action should have been triggered")
    }
}