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

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_create_task_list_dialog_confirm
import net.opatry.tasks.resources.task_lists_screen_create_task_list_dialog_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

@Composable
fun NewTaskListDialog(
    defaultTitle: String = "",
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
) {
    EditTextDialog(
        onDismissRequest = onDismissRequest,
        validateLabel = stringResource(Res.string.task_lists_screen_create_task_list_dialog_confirm),
        onValidate = onCreate,
        dialogTitle = stringResource(Res.string.task_lists_screen_create_task_list_dialog_title),
        initialText = defaultTitle,
        allowBlank = false,
    )
}

private class DefaultTitlePreviewParameterProvider(
    override val values: Sequence<String> = sequenceOf(
        "",
        "My task list",
    )
) : PreviewParameterProvider<String>

@Preview
@Composable
private fun NewTaskListDialogPreview(
    @PreviewParameter(DefaultTitlePreviewParameterProvider::class)
    defaultTitle: String,
) {
    TaskfolioThemedPreview(Modifier.size(500.dp, 300.dp)) {
        NewTaskListDialog(
            onDismissRequest = {},
            onCreate = {},
            defaultTitle = defaultTitle,
        )
    }
}
