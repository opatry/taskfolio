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

package net.opatry.tasks.app.ui.component

import CalendarDays
import ListPlus
import LucideIcons
import NotepadText
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.BOTTOM_SHEET
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.CANCEL_BUTTON
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.NOTES_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.SHEET_TITLE
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD_ERROR_MESSAGE
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.VALIDATE_BUTTON
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.task_editor_sheet_edit_title
import net.opatry.tasks.resources.task_editor_sheet_list_dropdown_label
import net.opatry.tasks.resources.task_editor_sheet_new_subtask_title
import net.opatry.tasks.resources.task_editor_sheet_new_task_title
import net.opatry.tasks.resources.task_editor_sheet_no_due_date_fallback
import net.opatry.tasks.resources.task_editor_sheet_notes_field_label
import net.opatry.tasks.resources.task_editor_sheet_notes_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_title_field_empty_error
import net.opatry.tasks.resources.task_editor_sheet_title_field_label
import net.opatry.tasks.resources.task_editor_sheet_title_field_placeholder
import net.opatry.tasks.resources.task_editor_sheet_validate
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting
object TaskEditorBottomSheetTestTag {
    const val BOTTOM_SHEET = "TASK_EDITOR_BOTTOM_SHEET"
    const val SHEET_TITLE = "TASK_EDITOR_BOTTOM_SHEET_TITLE"
    const val TITLE_FIELD = "TASK_EDITOR_TITLE_FIELD"
    const val TITLE_FIELD_ERROR_MESSAGE = "TASK_EDITOR_TITLE_FIELD_ERROR_MESSAGE"
    const val NOTES_FIELD = "TASK_EDITOR_NOTES_FIELD"
    const val DUE_DATE_CHIP = "TASK_EDITOR_DUE_DATE_CHIP"
    const val CANCEL_BUTTON = "TASK_EDITOR_CANCEL_BUTTON"
    const val VALIDATE_BUTTON = "TASK_EDITOR_VALIDATE_BUTTON"
}

enum class TaskEditMode {
    Edit,
    NewTask,
    NewSubTask,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorBottomSheet(
    editMode: TaskEditMode,
    task: TaskUIModel?,
    allTaskLists: List<TaskListUIModel>,
    selectedTaskList: TaskListUIModel,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onEditDueDate: () -> Unit,
    onValidate: (TaskListUIModel, String, String, LocalDate?) -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.testTag(BOTTOM_SHEET),
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        val sheetTitleRes = when (editMode) {
            TaskEditMode.Edit -> Res.string.task_editor_sheet_edit_title
            TaskEditMode.NewSubTask -> Res.string.task_editor_sheet_new_subtask_title
            TaskEditMode.NewTask -> Res.string.task_editor_sheet_new_task_title
        }
        // FIXME remembers from 1 dialog to the other
        val initialTitle = task?.title.takeUnless { editMode == TaskEditMode.NewSubTask }.orEmpty()
        var newTitle by remember { mutableStateOf(initialTitle) }
        // avoid displaying an error message when user didn't even started to write content
        var alreadyHadSomeContent by remember { mutableStateOf(initialTitle.isNotBlank()) }
        val titleHasError by remember {
            derivedStateOf {
                newTitle.isBlank()
            }
        }
        // FIXME remembers from 1 dialog to the other
        val initialNotes = task?.notes?.takeUnless { editMode == TaskEditMode.NewSubTask }.orEmpty()
        var newNotes by remember { mutableStateOf(initialNotes) }
        var expandTaskListsDropDown by remember { mutableStateOf(false) }
        var targetList by remember { mutableStateOf(selectedTaskList) }

        // FIXME doesn't work as expected BottomSheetDefaults.windowInsets.asPaddingValues()
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(sheetTitleRes),
                modifier = Modifier.testTag(SHEET_TITLE),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                newTitle,
                onValueChange = {
                    alreadyHadSomeContent = alreadyHadSomeContent || it.isNotBlank()
                    newTitle = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TITLE_FIELD),
                label = { Text(stringResource(Res.string.task_editor_sheet_title_field_label)) },
                placeholder = { Text(stringResource(Res.string.task_editor_sheet_title_field_placeholder)) },
                maxLines = 1,
                supportingText = {
                    AnimatedVisibility(visible = titleHasError && alreadyHadSomeContent) {
                        Text(
                            text = stringResource(Res.string.task_editor_sheet_title_field_empty_error),
                            modifier = Modifier.testTag(TITLE_FIELD_ERROR_MESSAGE),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                isError = titleHasError && alreadyHadSomeContent,
            )

            OutlinedTextField(
                newNotes,
                onValueChange = { newNotes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NOTES_FIELD),
                label = { Text(stringResource(Res.string.task_editor_sheet_notes_field_label)) },
                placeholder = { Text(stringResource(Res.string.task_editor_sheet_notes_field_placeholder)) },
                leadingIcon = { Icon(LucideIcons.NotepadText, null) },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
            )

            // TODO one of top of the other or side by side Date|TaskList
            //  if one on top of the other, need to revise expansion height to display all content
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TODO Add shortcuts for Today, Tomorrow, Next Week
                // FIXME when dialog is dismissed, current state is reset but shouldn't need to extract date picker dialog use
                val dueDateLabel = task?.dateRange?.toLabel()?.takeUnless(String::isBlank)
                    ?: stringResource(Res.string.task_editor_sheet_no_due_date_fallback)
                AssistChip(
                    onClick = onEditDueDate,
                    modifier = Modifier.testTag(DUE_DATE_CHIP),
                    enabled = false, // TODO not supported for now, super imposed dialogs breaks the flow
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(LucideIcons.CalendarDays, null) },
                    label = { Text(dueDateLabel, color = task?.dateRange.toColor()) },
                )

                val enableAdvancedTaskEdit = false // TODO implement advanced task edit
                if (enableAdvancedTaskEdit && editMode == TaskEditMode.Edit) {
                    ExposedDropdownMenuBox(
                        expanded = expandTaskListsDropDown,
                        onExpandedChange = { expandTaskListsDropDown = it }
                    ) {
                        OutlinedTextField(
                            targetList.title,
                            onValueChange = {},
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            label = { Text(stringResource(Res.string.task_editor_sheet_list_dropdown_label)) },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandTaskListsDropDown)
                            },
                        )

                        ExposedDropdownMenu(
                            expanded = expandTaskListsDropDown,
                            onDismissRequest = { expandTaskListsDropDown = false }
                        ) {
                            allTaskLists.forEach { taskList ->
                                DropdownMenuItem(
                                    text = { Text(taskList.title) },
                                    onClick = {
                                        targetList = taskList
                                        expandTaskListsDropDown = false
                                    }
                                )
                            }
                        }
                    }

                    // TODO instead of a button opening a dialog, TextField could be editable to let user
                    //  enter the new list name, if targetList isn't part of taskLists, then, it's a new one
                    IconButton(onClick = {}) {
                        Icon(LucideIcons.ListPlus, null)
                    }
                }
            }

            Row(
                Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.dialog_cancel),
                        modifier = Modifier.testTag(CANCEL_BUTTON),
                    )
                }
                Button(
                    onClick = {
                        onValidate(targetList, newTitle, newNotes, task?.dueDate)
                    },
                    modifier = Modifier.testTag(VALIDATE_BUTTON),
                    enabled = newTitle.isNotBlank()
                ) {
                    Text(stringResource(Res.string.task_editor_sheet_validate))
                }
            }
        }
    }
}