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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.LocalDate
import net.opatry.tasks.app.ui.component.CompletedTaskRow
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_COMPLETION_DATE
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_DELETE_ICON
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ICON
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_NOTES
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ROW
import net.opatry.tasks.app.ui.component.TaskAction
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_pane_completed_date_label
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class CompletedTaskRowTest {
    @Test
    fun CompletedTaskRow_Layout() = runComposeUiTest {
        val task = createTask("My Completed Task", isCompleted = true).copy(
            notes = "",
            completionDate = null
        )
        setContent {
            CompletedTaskRow(task) {}
        }

        onNodeWithText("My Completed Task")
            .assertIsDisplayed()

        onNodeWithTag(COMPLETED_TASK_NOTES)
            .assertDoesNotExist()

        onNodeWithTag(COMPLETED_TASK_COMPLETION_DATE)
            .assertDoesNotExist()
    }

    @Test
    fun CompletedTaskRow_LayoutFull() = runComposeUiTest {
        val task = createTask("My Completed Task With notes & date", isCompleted = true).copy(
            notes = "My notes",
            completionDate = LocalDate.parse("2023-10-01")
        )
        lateinit var dateStr: String
        setContent {
            dateStr = stringResource(Res.string.task_list_pane_completed_date_label, "October 1, 2023")
            CompletedTaskRow(task) {}
        }

        onNodeWithText("My Completed Task With notes & date")
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(COMPLETED_TASK_NOTES, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("My notes")

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(COMPLETED_TASK_COMPLETION_DATE, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals(dateStr)
    }

    @Test
    fun CompletedTaskRow_UnComplete() = runComposeUiTest {
        val task = createTask(isCompleted = true)
        var action: TaskAction? = null
        setContent {
            CompletedTaskRow(task) {
                action = it
            }
        }

        onNodeWithTag(COMPLETED_TASK_ICON)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.ToggleCompletion(task), action, "Toggle completion action should have been triggered")
    }

    @Test
    fun CompletedTaskRow_Delete() = runComposeUiTest {
        val task = createTask(isCompleted = true)
        var action: TaskAction? = null
        setContent {
            CompletedTaskRow(task) {
                action = it
            }
        }

        onNodeWithTag(COMPLETED_TASK_DELETE_ICON)
            .assertIsDisplayed()
            .performClick()

        assertEquals(TaskAction.Delete(task), action, "Delete action should have been triggered")
    }

    @Test
    fun CompletedTaskRow_Edit() = runComposeUiTest {
        val task = createTask(isCompleted = true)
        var action: TaskAction? = null
        setContent {
            CompletedTaskRow(task) {
                action = it
            }
        }

        onNodeWithTag(COMPLETED_TASK_ROW)
            .performClick()

        assertEquals(TaskAction.Edit(task), action, "Click on cell should trigger Edit action")
    }
}