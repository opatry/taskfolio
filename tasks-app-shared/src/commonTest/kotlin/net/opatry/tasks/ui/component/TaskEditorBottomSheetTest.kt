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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskEditMode
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheet
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.CANCEL_BUTTON
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.NOTES_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.SHEET_TITLE
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD_ERROR_MESSAGE
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.VALIDATE_BUTTON
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_tomorrow
import net.opatry.tasks.resources.task_editor_sheet_edit_title
import net.opatry.tasks.resources.task_editor_sheet_new_subtask_title
import net.opatry.tasks.resources.task_editor_sheet_new_task_title
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class TaskEditorBottomSheetTest {
    @Test
    fun `when editMode is NewTask then title should be for new task`() = runComposeUiTest {
        lateinit var newTaskSheetTitle: String
        setContent {
            newTaskSheetTitle = stringResource(Res.string.task_editor_sheet_new_task_title)
            TaskEditorBottomSheet(
                editMode = TaskEditMode.NewTask,
                task = null,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, _, _, _ ->},
            )
        }

        onNodeWithTag(SHEET_TITLE)
            .assertIsDisplayed()
            .assertTextEquals(newTaskSheetTitle)

        onNodeWithTag(TITLE_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("")

        onNodeWithTag(NOTES_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("")

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun `when editMode is Edit then title should be for task edit`() = runComposeUiTest {
        lateinit var editTaskSheetTitle: String
        setContent {
            editTaskSheetTitle = stringResource(Res.string.task_editor_sheet_edit_title)
            TaskEditorBottomSheet(
                editMode = TaskEditMode.Edit,
                task = TaskUIModel.Todo(
                    id = TaskId(1L),
                    title = "Task title",
                    notes = "Task notes",
                ),
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, _, _, _ -> },
            )
        }

        onNodeWithTag(SHEET_TITLE)
            .assertIsDisplayed()
            .assertTextEquals(editTaskSheetTitle)

        onNodeWithTag(TITLE_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Task title")

        onNodeWithTag(NOTES_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Task notes")

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsEnabled()
    }

    @Test
    fun `when editMode is NewSubTask then title should be for new subtask`() = runComposeUiTest {
        lateinit var editTaskSheetTitle: String
        setContent {
            editTaskSheetTitle = stringResource(Res.string.task_editor_sheet_new_subtask_title)
            TaskEditorBottomSheet(
                editMode = TaskEditMode.NewSubTask,
                task = null,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, _, _, _ -> },
            )
        }

        onNodeWithTag(SHEET_TITLE)
            .assertIsDisplayed()
            .assertTextEquals(editTaskSheetTitle)
    }

    @Test
    fun `when editing task with title, notes and due date then all fields should be displayed`() = runComposeUiTest {
        val task = TaskUIModel.Todo(
            id = TaskId(1L),
            title = "Task title",
            notes = "Task notes",
            dueDate = Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.DAY),
        )
        lateinit var tomorrowStr: String
        setContent {
            tomorrowStr = stringResource(Res.string.task_due_date_label_tomorrow)
            TaskEditorBottomSheet(
                editMode = TaskEditMode.Edit,
                task = task,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, _, _, _ -> },
            )
        }

        onNodeWithTag(TITLE_FIELD, useUnmergedTree = true)
            .assertTextEquals("Task title")

        onNodeWithTag(TITLE_FIELD_ERROR_MESSAGE)
            .assertDoesNotExist()

        onNodeWithTag(NOTES_FIELD, useUnmergedTree = true)
            .assertTextEquals("Task notes")

        onNodeWithTag(DUE_DATE_CHIP)
            .assertTextEquals(tomorrowStr)
    }

    @Test
    fun `when emptying title then error message should be displayed and validate button disabled`() = runComposeUiTest {
        val task = TaskUIModel.Todo(
            id = TaskId(1L),
            title = "Task title",
        )
        setContent {
            TaskEditorBottomSheet(
                editMode = TaskEditMode.Edit,
                task = task,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, _, _, _ -> },
            )
        }

        onNodeWithTag(TITLE_FIELD, useUnmergedTree = true)
            .performTextClearance()

        onNodeWithTag(TITLE_FIELD_ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun `when validating update then updated values should be notified`() = runComposeUiTest {
        val task = TaskUIModel.Todo(
            id = TaskId(1L),
            title = "Task title",
            notes = "Task notes",
        )
        var newTitle: String? = null
        var newNotes: String? = null
        setContent {
            TaskEditorBottomSheet(
                editMode = TaskEditMode.Edit,
                task = task,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {},
                onEditDueDate = {},
                onValidate = { _, title, notes, _ ->
                    newTitle = title
                    newNotes = notes
                },
            )
        }

        onNodeWithTag(TITLE_FIELD)
            // FIXME cursor should already be at the end of the text field
            .performTextInputSelection(TextRange(task.title.length))
        onNodeWithTag(TITLE_FIELD)
            .performTextInput(" and more")

        onNodeWithTag(NOTES_FIELD)
            .performTextClearance()

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsEnabled()
            .performClick()

        assertEquals("Task title and more", newTitle)
        assertEquals("", newNotes)
    }

    @Test
    fun `when canceling then sheet should be dismissed`() = runComposeUiTest {
        val task = TaskUIModel.Todo(
            id = TaskId(1L),
            title = "Task title",
            notes = "Task notes",
        )
        var isDismissed = false
        var validatedCalled = false
        setContent {
            TaskEditorBottomSheet(
                editMode = TaskEditMode.Edit,
                task = task,
                allTaskLists = emptyList(),
                selectedTaskList = TaskListUIModel(id = TaskListId(100), "Tasks"),
                onDismiss = {
                    isDismissed = true
                },
                onEditDueDate = {},
                onValidate = { _, _, _, _ ->
                    validatedCalled = true
                },
            )
        }

        onNodeWithTag(CANCEL_BUTTON, useUnmergedTree = true)
            .assertIsEnabled()
            .performClick()

        assertTrue(isDismissed)
        assertFalse(validatedCalled)
    }
}
