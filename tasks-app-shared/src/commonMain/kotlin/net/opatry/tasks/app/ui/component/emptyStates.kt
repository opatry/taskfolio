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
import androidx.annotation.VisibleForTesting
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_DELETE_BUTTON
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_EMPTY_STATE
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_REPAIR_BUTTON
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASKS_EMPTY_STATE
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASK_LISTS_EMPTY_STATE
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASK_LIST_SELECTED_EMPTY_STATE
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_lists_screen_empty_list_desc
import net.opatry.tasks.resources.task_lists_screen_empty_list_title
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
import org.jetbrains.compose.ui.tooling.preview.Preview

@VisibleForTesting
object EmptyStatesTestTag {
    const val NO_TASKS_EMPTY_STATE = "NO_TASKS_EMPTY_STATE"
    const val NO_TASK_LIST_SELECTED_EMPTY_STATE = "NO_TASK_LIST_SELECTED_EMPTY_STATE"
    const val NO_TASK_LISTS_EMPTY_STATE = "NO_TASK_LISTS_EMPTY_STATE"
    const val NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON = "NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON"
    const val BROKEN_LIST_EMPTY_STATE = "BROKEN_LIST_EMPTY_STATE"
    const val BROKEN_LIST_DELETE_BUTTON = "BROKEN_LIST_DELETE_BUTTON"
    const val BROKEN_LIST_REPAIR_BUTTON = "BROKEN_LIST_REPAIR_BUTTON"
}

@Composable
fun NoTaskListSelectedEmptyState() {
    EmptyState(
        icon = LucideIcons.CircleOff,
        title = stringResource(Res.string.task_lists_screen_empty_state_no_selection_title),
        description = stringResource(Res.string.task_lists_screen_empty_state_no_selection_desc),
        modifier = Modifier.fillMaxSize().testTag(NO_TASK_LIST_SELECTED_EMPTY_STATE)
    )
}

@Composable
fun NoTasksEmptyState() {
    // TODO SVG undraw.co illustration `files/undraw_to_do_list_re_9nt7.svg`
    EmptyState(
        icon = LucideIcons.CircleOff,
        title = stringResource(Res.string.task_lists_screen_empty_list_title),
        description = stringResource(Res.string.task_lists_screen_empty_list_desc),
        modifier = Modifier
            .fillMaxSize()
            .testTag(NO_TASKS_EMPTY_STATE),
    )
}

@Composable
fun NoTaskListsEmptyState(onNewTaskListClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().testTag(NO_TASK_LISTS_EMPTY_STATE),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = LucideIcons.CheckCheck,
            title = stringResource(Res.string.task_lists_screen_empty_state_title),
            description = stringResource(Res.string.task_lists_screen_empty_state_desc),
            modifier = Modifier.fillMaxWidth(1f)
        )
        Button(
            onClick = onNewTaskListClick,
            modifier = Modifier.testTag(NO_TASK_LISTS_EMPTY_STATE_CREATE_LIST_BUTTON),
        ) {
            Text(stringResource(Res.string.task_lists_screen_empty_state_cta))
        }
    }
}

@Composable
fun BrokenListIndentationEmptyState(onDeleteList: () -> Unit, onRepairList: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp).testTag(BROKEN_LIST_EMPTY_STATE),
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
            TextButton(
                onClick = onDeleteList,
                modifier = Modifier.testTag(BROKEN_LIST_DELETE_BUTTON)
            ) {
                Text(stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_delete_cta))
            }
            // TODO enable when broken indentation reparation is available
            TextButton(
                onClick = onRepairList,
                modifier = Modifier.testTag(BROKEN_LIST_REPAIR_BUTTON),
                enabled = false,
            ) {
                Text(stringResource(Res.string.task_lists_screen_empty_state_broken_list_indent_repair_cta))
            }
        }
    }
}

@Preview
@Composable
private fun NoTaskListSelectedEmptyStatePreview() {
    TaskfolioThemedPreview {
        NoTaskListSelectedEmptyState()
    }
}

@Preview
@Composable
private fun NoTasksEmptyStatePreview() {
    TaskfolioThemedPreview {
        NoTasksEmptyState()
    }
}

@Preview
@Composable
private fun NoTaskListEmptyStatePreview() {
    TaskfolioThemedPreview {
        NoTaskListsEmptyState {}
    }
}

@Preview
@Composable
private fun BrokenListIndentationEmptyStatePreview() {
    TaskfolioThemedPreview {
        BrokenListIndentationEmptyState({}, {})
    }
}