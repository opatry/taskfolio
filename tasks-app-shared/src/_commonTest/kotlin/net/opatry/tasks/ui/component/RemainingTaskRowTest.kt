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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import net.opatry.tasks.app.ui.component.DueDateUpdate
import net.opatry.tasks.app.ui.component.RemainingTaskRow
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.MENU_ICON
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.NOTES
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.RESET_DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.ROW
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_DUE_DATE_TOMORROW_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_TASK_DUE_DATE_TODAY_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.TOGGLE_ICON
import net.opatry.tasks.app.ui.component.TaskAction
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.ADD_SUBTASK
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.DELETE
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.INDENT
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_NEW_LIST
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.MOVE_TO_TOP
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.TASK_MENU
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.UNINDENT
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_weeks_ago
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.test.Test
import kotlin.test.assertEquals


@OptIn(ExperimentalTestApi::class)
class RemainingTaskRowTest {
    @Test
    fun `when task has no notes and no due date then row should have title and no notes nor due date chip`() = runComposeUiTest {
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

        onNodeWithTag(NOTES)
            .assertDoesNotExist()

        onNodeWithTag(DUE_DATE_CHIP)
            .assertDoesNotExist()
    }

    @Test
    fun `when task has notes and due date then row should have title, notes and date chip`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first().copy(
            title = "My Task with notes & date",
            notes = "My notes",
            dueDate = today.minus(2, DateTimeUnit.WEEK)
        )
        lateinit var twoWeeksAgo: String
        setContent {
            twoWeeksAgo = pluralStringResource(Res.plurals.task_due_date_label_weeks_ago, 2, 2)
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithText("My Task with notes & date")
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(NOTES, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("My notes")

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(DUE_DATE_CHIP, useUnmergedTree = true)
            .assertIsDisplayed()
            .onChildren()
            .assertCountEquals(1)
            .onFirst()
            .assertTextEquals(twoWeeksAgo)
    }

    @Test
    fun `when clicking task TOGGLE_ICON then should trigger ToggleCompletion action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(TOGGLE_ICON)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.ToggleCompletion(task), action, "Toggle completion action should have been triggered")
    }

    @Test
    fun `when clicking MENU_ICON then should display task menu`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(TASK_MENU)
            .assertDoesNotExist()

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(TASK_MENU)
            .assertIsDisplayed()
    }

    @Test
    fun `when clicking a task row then should trigger Edit action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(ROW)
            .performClick()

        assertEquals(TaskAction.Edit(task), action, "Click on row should trigger Edit action")
    }

    @Test
    fun `when task with due date then should have due date and reset chips`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(DUE_DATE_CHIP)
            .assertIsDisplayed()
            .assertIsEnabled()
        onNodeWithTag(RESET_DUE_DATE_CHIP)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(SET_TASK_DUE_DATE_TODAY_CHIP)
            .assertDoesNotExist()
        onNodeWithTag(SET_DUE_DATE_TOMORROW_CHIP)
            .assertDoesNotExist()
        onNodeWithTag(SET_DUE_DATE_CHIP)
            .assertDoesNotExist()
    }

    @Test
    fun `when clicking DUE_DATE_CHIP then should trigger UpdateDueDate with Pick action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(DUE_DATE_CHIP)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.UpdateDueDate(task, DueDateUpdate.Pick), action, "UpdateDueDate Pick action should have been triggered")
    }

    @Test
    fun `when task without due date then should have edit shortcut chips`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
            .copy(dueDate = null)
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(DUE_DATE_CHIP)
            .assertDoesNotExist()
        onNodeWithTag(RESET_DUE_DATE_CHIP)
            .assertDoesNotExist()

        onNodeWithTag(SET_TASK_DUE_DATE_TODAY_CHIP)
            .assertIsDisplayed()
            .assertIsEnabled()
        onNodeWithTag(SET_DUE_DATE_TOMORROW_CHIP)
            .assertIsDisplayed()
            .assertIsEnabled()
        onNodeWithTag(SET_DUE_DATE_CHIP)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when clicking RESET_DUE_DATE_CHIP then should trigger UpdateDueDate with Reset action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(RESET_DUE_DATE_CHIP)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.UpdateDueDate(task, DueDateUpdate.Reset), action, "UpdateDueDate Reset action should have been triggered")
    }

    @Test
    fun `when clicking SET_TASK_DUE_DATE_TODAY_CHIP then should trigger UpdateDueDate with Today action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
            .copy(dueDate = null)
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(SET_TASK_DUE_DATE_TODAY_CHIP)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.UpdateDueDate(task, DueDateUpdate.Today), action, "UpdateDueDate Today action should have been triggered")
    }

    @Test
    fun `when clicking SET_DUE_DATE_TOMORROW_CHIP then should trigger UpdateDueDate with Tomorrow action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
            .copy(dueDate = null)
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(SET_DUE_DATE_TOMORROW_CHIP)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.UpdateDueDate(task, DueDateUpdate.Tomorrow), action, "UpdateDueDate Tomorrow action should have been triggered")
    }

    @Test
    fun `when clicking SET_DUE_DATE_CHIP then should trigger UpdateDueDate with Pick action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
            .copy(dueDate = null)
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(SET_DUE_DATE_CHIP)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.UpdateDueDate(task, DueDateUpdate.Pick), action, "UpdateDueDate Pick action should have been triggered")
    }

    @Test
    fun `when canCreateSubTask=false then ADD_SUBTASK menu should be hidden`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canCreateSubTask = false)))
        )
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(ADD_SUBTASK)
            .assertDoesNotExist()
    }

    @Test
    fun `when canCreateSubTask=true then ADD_SUBTASK menu should be enabled and trigger AddSubTask action`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canCreateSubTask = true)))
        )
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(ADD_SUBTASK)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.AddSubTask(task), action, "AddSubTask action should have been triggered")
    }

    @Test
    fun `when canMoveToTop=false then MOVE_TO_TOP menu should be hidden`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canMoveToTop = false)))
        )
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(MOVE_TO_TOP)
            .assertDoesNotExist()
    }

    @Test
    fun `when canMoveToTop=true then MOVE_TO_TOP menu should be enabled and trigger MoveToTop action`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canMoveToTop = true)))
        )
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(MOVE_TO_TOP)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.MoveToTop(task), action, "MoveToTop action should have been triggered")
    }

    @Test
    fun `when canUnindent=false then UNINDENT menu should be hidden`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canUnindent = false)))
        )
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(UNINDENT)
            .assertDoesNotExist()
    }

    @Test
    fun `when canUnindent=true then UNINDENT menu should be enabled and trigger Unindent action`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canUnindent = true)))
        )
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(UNINDENT)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.Unindent(task), action, "Unindent action should have been triggered")
    }

    @Test
    fun `when canIndent=false then INDENT menu should be hidden`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canIndent = false)))
        )
        val task = taskList.allRemainingTasks.first()
        setContent {
            RemainingTaskRow(listOf(taskList), task) {}
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(INDENT)
            .assertDoesNotExist()
    }

    @Test
    fun `when canIndent=true then INDENT menu should be enabled and trigger Indent action`() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(null to listOf(createTask(canIndent = true)))
        )
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(INDENT)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.Indent(task), action, "Indent action should have been triggered")
    }

    @Test
    fun `when clicking on MOVE_TO_NEW_LIST menu then should trigger MoveToNewList action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(MOVE_TO_NEW_LIST)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.MoveToNewList(task), action, "MoveToNewList action should have been triggered")
    }

    @Test
    fun `when clicking on MOVE_TO_LIST menu then should trigger MoveToList action with chosen list`() = runComposeUiTest {
        val taskList1 = createTaskList(title = "list1", remainingTaskCount = 1)
        val taskList2 = createTaskList(title = "list2")
        val task = taskList1.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList1, taskList2), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onAllNodesWithTag(MOVE_TO_LIST)
            .assertCountEquals(2)
            .onFirst()
            .assertTextEquals(taskList1.title)
            .assertIsNotEnabled()

        onAllNodesWithTag(MOVE_TO_LIST)
            .onLast()
            .assertTextEquals(taskList2.title)
            .assertIsEnabled()
            .performClick()

        assertEquals(TaskAction.MoveToList(task, taskList2), action, "MoveToList action should have been triggered on list2")
    }

    @Test
    fun `when clicking on DELETE menu then should trigger the Delete action`() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1)
        val task = taskList.allRemainingTasks.first()
        var action: TaskAction? = null
        setContent {
            RemainingTaskRow(listOf(taskList), task) {
                action = it
            }
        }

        onNodeWithTag(MENU_ICON)
            .assertIsDisplayed()
            .performClick()

        onNodeWithTag(DELETE)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.Delete(task), action, "Delete action should have been triggered")
    }
}