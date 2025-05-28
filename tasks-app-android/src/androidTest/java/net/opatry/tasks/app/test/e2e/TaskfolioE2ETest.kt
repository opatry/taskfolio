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

package net.opatry.tasks.app.test.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.opatry.tasks.app.MainActivity
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASKS_EMPTY_STATE
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON
import net.opatry.tasks.app.ui.component.TaskListScaffoldTestTag.ADD_TASK_FAB
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.ALL_TASKS_COMPLETED_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE
import org.junit.Test
import org.junit.runner.RunWith
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ICON as COMPLETED_TASK_TOGGLE_ICON
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.TEXT_FIELD as CREATE_TASK_LIST_DIALOG_TEXT_FIELD
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.VALIDATE_BUTTON as CREATE_TASK_LIST_DIALOG_VALIDATE_BUTTON
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.MENU_ICON as REMAINING_TASK_MENU_ICON
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.TOGGLE_ICON as REMAINING_TASK_TOGGLE_ICON
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.NOTES_FIELD as TASK_NOTES_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD as TASK_TITLE_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.VALIDATE_BUTTON as CREATE_TASK_SHEET_VALIDATE_BUTTON
import net.opatry.tasks.app.ui.component.TaskMenuTestTag.DELETE as REMAINING_TASK_DELETE_MENU_ITEM
import net.opatry.tasks.app.ui.screen.AuthorizationScreenTestTags.SKIP_BUTTON as AUTHORIZATION_SCREEN_SKIP_BUTTON

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TaskfolioE2ETest {

    @Test
    fun testTaskfolioHappyPath() = runAndroidComposeUiTest<MainActivity> {
        // FIXME string resources from :tasks-app-shared?
        // FIXME replace testTag with string resources?

        // skip authorization screen
        waitUntilExactlyOneExists(hasTestTag(AUTHORIZATION_SCREEN_SKIP_BUTTON))
        onNodeWithTag(AUTHORIZATION_SCREEN_SKIP_BUTTON)
            .assertIsDisplayed()
            .performClick()

        // create the first task list
        waitUntilExactlyOneExists(hasTestTag(NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON))
        onNodeWithTag(NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON)
            .assertIsDisplayed()
            .performClick()

        waitUntilExactlyOneExists(hasTestTag(CREATE_TASK_LIST_DIALOG_TEXT_FIELD))
        onNodeWithTag(CREATE_TASK_LIST_DIALOG_TEXT_FIELD)
            .performTextClearance()

        val listTitle = "My list ✅"
        onNodeWithTag(CREATE_TASK_LIST_DIALOG_TEXT_FIELD)
            .performTextInput(listTitle)

        waitUntilExactlyOneExists(hasTestTag(CREATE_TASK_LIST_DIALOG_VALIDATE_BUTTON))
        onNodeWithTag(CREATE_TASK_LIST_DIALOG_VALIDATE_BUTTON)
            .performClick()

        // creating a list switches to the tasks screen automatically
        waitUntilExactlyOneExists(hasTestTag(NO_TASKS_EMPTY_STATE))
        onNodeWithTag(NO_TASKS_EMPTY_STATE)
            .assertIsDisplayed()
        onNodeWithText(listTitle)
            .assertIsDisplayed()

        // add a task to the list
        waitUntilExactlyOneExists(hasTestTag(ADD_TASK_FAB))
        onNodeWithTag(ADD_TASK_FAB)
            .performClick()

        val taskTitle = "My Wonderful task 🌈"
        waitUntilExactlyOneExists(hasTestTag(TASK_TITLE_FIELD))
        onNodeWithTag(TASK_TITLE_FIELD)
            .performTextInput(taskTitle)

        val taskNotes = "Some notes:\n• with a list\n• and some emojis 😊"
        waitUntilExactlyOneExists(hasTestTag(TASK_NOTES_FIELD))
        onNodeWithTag(TASK_NOTES_FIELD)
            .performTextInput(taskNotes)

        waitUntilExactlyOneExists(hasTestTag(CREATE_TASK_SHEET_VALIDATE_BUTTON))
        onNodeWithTag(CREATE_TASK_SHEET_VALIDATE_BUTTON)
            .performClick()

        waitUntilExactlyOneExists(hasText(taskTitle))
        onNodeWithText(taskTitle)
            .assertIsDisplayed()
        onNodeWithText(taskNotes)
            .assertIsDisplayed()

        // mark the task as completed
        waitUntilExactlyOneExists(hasTestTag(REMAINING_TASK_TOGGLE_ICON))
        onNodeWithTag(REMAINING_TASK_TOGGLE_ICON)
            .assertIsDisplayed()
            .performClick()

        waitUntilExactlyOneExists(hasTestTag(ALL_TASKS_COMPLETED_EMPTY_STATE))
        onNodeWithTag(ALL_TASKS_COMPLETED_EMPTY_STATE)
            .assertIsDisplayed()
        onNodeWithText(taskTitle)
            .assertDoesNotExist()

        // expand completed tasks area
        waitUntilExactlyOneExists(hasTestTag(COMPLETED_TASKS_TOGGLE))
        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
            .performClick()

        waitUntilExactlyOneExists(hasTestTag(COMPLETED_TASK_TOGGLE_ICON))
        onNodeWithText(taskTitle)
            .assertIsDisplayed()

        // restore completed task as remaining task
        onNodeWithTag(COMPLETED_TASK_TOGGLE_ICON)
            .performClick()

        waitUntilDoesNotExist(hasTestTag(COMPLETED_TASKS_TOGGLE))
        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertDoesNotExist()

        onNodeWithTag(ALL_TASKS_COMPLETED_EMPTY_STATE)
            .assertDoesNotExist()
        onNodeWithTag(REMAINING_TASK_TOGGLE_ICON)
            .assertIsDisplayed()
        onNodeWithText(taskTitle)
            .assertIsDisplayed()

        // delete the task
        onNodeWithTag(REMAINING_TASK_MENU_ICON)
            .performClick()

        waitUntilExactlyOneExists(hasTestTag(REMAINING_TASK_DELETE_MENU_ITEM))
        onNodeWithTag(REMAINING_TASK_DELETE_MENU_ITEM)
            .performClick()

        waitUntilExactlyOneExists(hasTestTag(NO_TASKS_EMPTY_STATE))
        onNodeWithTag(NO_TASKS_EMPTY_STATE)
            .assertIsDisplayed()
    }
}