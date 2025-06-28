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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.CANCEL_BUTTON
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.DIALOG_TITLE
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.ERROR_MESSAGE
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.TEXT_FIELD
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.VALIDATE_BUTTON
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.edit_text_dialog_empty_title_error
import net.opatry.tasks.resources.edit_text_dialog_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@VisibleForTesting
object EditTextDialogTestTag {
    const val DIALOG_TITLE = "EDIT_TEXT_DIALOG_TITLE"
    const val TEXT_FIELD = "EDIT_TEXT_DIALOG_TEXT_FIELD"
    const val ERROR_MESSAGE = "EDIT_TEXT_DIALOG_ERROR_MESSAGE"
    const val CANCEL_BUTTON = "EDIT_TEXT_DIALOG_CANCEL_BUTTON"
    const val VALIDATE_BUTTON = "EDIT_TEXT_DIALOG_VALIDATE_BUTTON"
}

@Composable
fun EditTextDialog(
    onDismissRequest: () -> Unit,
    validateLabel: String,
    onValidate: (String) -> Unit,
    dialogTitle: String? = null,
    initialText: String = "",
    allowBlank: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        var titleFieldState by remember {
            mutableStateOf(
                TextFieldValue(
                    text = initialText,
                    selection = TextRange(initialText.length)
                )
            )
        }
        // avoid displaying an error message when user didn't even started to write content
        var alreadyHadSomeContent by remember { mutableStateOf(initialText.isNotBlank()) }
        val hasError by remember(titleFieldState, allowBlank) {
            derivedStateOf {
                !allowBlank && titleFieldState.text.isBlank()
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (dialogTitle != null) {
                    Text(
                        text = dialogTitle,
                        modifier = Modifier.testTag(DIALOG_TITLE),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                OutlinedTextField(
                    value = titleFieldState,
                    onValueChange = {
                        alreadyHadSomeContent = alreadyHadSomeContent || it.text.isNotBlank()
                        titleFieldState = it
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag(TEXT_FIELD),
                    label = { Text(stringResource(Res.string.edit_text_dialog_title)) },
                    maxLines = 1,
                    supportingText = if (allowBlank) null else {
                        {
                            AnimatedVisibility(visible = hasError && alreadyHadSomeContent) {
                                Text(
                                    text = stringResource(Res.string.edit_text_dialog_empty_title_error),
                                    modifier = Modifier.testTag(ERROR_MESSAGE)
                                )
                            }
                        }
                    },
                    isError = hasError && alreadyHadSomeContent,
                )
                Row(
                    Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismissRequest, Modifier.testTag(CANCEL_BUTTON)) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                    Button(
                        onClick = { onValidate(titleFieldState.text) },
                        modifier = Modifier.testTag(VALIDATE_BUTTON),
                        enabled = allowBlank || !hasError
                    ) {
                        Text(validateLabel)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditTextDialogPreview() {
    TaskfolioThemedPreview(Modifier.size(500.dp, 300.dp)) {
        EditTextDialog(
            onDismissRequest = {},
            validateLabel = "OK",
            onValidate = {},
            dialogTitle = "My property",
            initialText = "My value",
            allowBlank = true,
        )
    }
}
