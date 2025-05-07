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

import EllipsisVertical
import LucideIcons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.model.TaskListId
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview

// FIXME When displayed with dark, the menu labels are invisible
//  keep only single light preview
@Preview(showBackground = true)
@Composable
private fun TaskListMenuPreview() {
    TaskfolioThemedPreview {
        Box(
            Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(24.dp), contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = {}) {
                Icon(LucideIcons.EllipsisVertical, null)
                TaskListMenu(
                    taskList = TaskListUIModel(
                        id = TaskListId(0L),
                        title = "My task list",
                        lastUpdate = "TODO DATE",
                    ),
                    expanded = true,
                    onAction = {}
                )
            }
        }
    }
}