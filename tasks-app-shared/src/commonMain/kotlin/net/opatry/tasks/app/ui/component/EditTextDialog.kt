/*
 * Copyright (c) 2024 Olivier Patry
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.dialog_cancel
import net.opatry.tasks.resources.edit_text_dialog_empty_title_error
import net.opatry.tasks.resources.edit_text_dialog_title
import org.jetbrains.compose.resources.stringResource

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
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        var newTitle by remember { mutableStateOf(initialText) }
        // avoid displaying an error message when user didn't even started to write content
        var alreadyHadSomeContent by remember { mutableStateOf(initialText.isNotBlank()) }
        val hasError by remember(newTitle, allowBlank) {
            derivedStateOf {
                !allowBlank && newTitle.isBlank()
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (dialogTitle != null) {
                    Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    newTitle,
                    onValueChange = {
                        alreadyHadSomeContent = alreadyHadSomeContent || it.isNotBlank()
                        newTitle = it
                    },
                    label = { Text(stringResource(Res.string.edit_text_dialog_title)) },
                    maxLines = 1,
                    supportingText = if (allowBlank) null else {
                        {
                            AnimatedVisibility(visible = hasError && alreadyHadSomeContent) {
                                Text(stringResource(Res.string.edit_text_dialog_empty_title_error))
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
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(Res.string.dialog_cancel))
                    }
                    Button(
                        onClick = { onValidate(newTitle) },
                        enabled = allowBlank || !hasError
                    ) {
                        Text(validateLabel)
                    }
                }
            }
        }
    }
}