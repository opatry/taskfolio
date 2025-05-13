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

import CheckCheck
import CircleOff
import LucideIcons
import Replace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_empty_state_broken_list_indent_delete_cta
import net.opatry.tasks.resources.task_lists_screen_empty_state_broken_list_indent_desc
import net.opatry.tasks.resources.task_lists_screen_empty_state_broken_list_indent_repair_cta
import net.opatry.tasks.resources.task_lists_screen_empty_state_broken_list_indent_title
import net.opatry.tasks.resources.task_lists_screen_empty_state_cta
import net.opatry.tasks.resources.task_lists_screen_empty_state_desc
import net.opatry.tasks.resources.task_lists_screen_empty_state_no_selection_desc
import net.opatry.tasks.resources.task_lists_screen_empty_state_no_selection_title
import net.opatry.tasks.resources.task_lists_screen_empty_state_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoTaskListSelectedEmptyState() {
    EmptyState(
        icon = LucideIcons.CircleOff,
        title = stringResource(Res.string.task_lists_screen_empty_state_no_selection_title),
        description = stringResource(Res.string.task_lists_screen_empty_state_no_selection_desc),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NoTaskListEmptyState(onNewTaskListClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = LucideIcons.CheckCheck,
            title = stringResource(Res.string.task_lists_screen_empty_state_title),
            description = stringResource(Res.string.task_lists_screen_empty_state_desc),
            modifier = Modifier.fillMaxWidth(1f)
        )
        Button(onClick = onNewTaskListClick) {
            Text(stringResource(Res.string.task_lists_screen_empty_state_cta))
        }
    }
}

@Composable
fun BrokenListIndentationEmptyState(onDeleteList: () -> Unit, onRepairList: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = LucideIcons.Replace,
            title = stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_title),
            description = stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_desc),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TextButton(onClick = onDeleteList) {
                Text(stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_delete_cta))
            }
            // TODO enable when broken indentation reparation is available
            TextButton(onClick = onRepairList, enabled = false) {
                Text(stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_repair_cta))
            }
        }
    }
}