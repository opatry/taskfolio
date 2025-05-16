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

import CircleCheckBig
import LucideIcons
import Trash
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_COMPLETION_DATE
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_DELETE_ICON
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ICON
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_NOTES
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ROW
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_list_pane_completed_date_label
import net.opatry.tasks.resources.task_list_pane_delete_task_icon_content_desc
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting
internal object CompletedTaskRowTestTag {
    const val COMPLETED_TASK_ROW = "COMPLETED_TASK_ROW"
    const val COMPLETED_TASK_ICON = "COMPLETED_TASK_ICON"
    const val COMPLETED_TASK_NOTES = "COMPLETED_TASK_NOTES"
    const val COMPLETED_TASK_COMPLETION_DATE = "COMPLETED_TASK_COMPLETION_DATE"
    const val COMPLETED_TASK_DELETE_ICON = "COMPLETED_TASK_DELETE_ICON"
}

private fun LocalDate.toLabel(): String {
    // TODO localize names & format
    return if (year == Clock.System.todayIn(TimeZone.currentSystemDefault()).year) {
        LocalDate.Format {
            // FIXME doesn't work with more than 2 dd or MM
            //  byUnicodePattern("ddd', 'MMM' 'yyyy")
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // TODO translation
            chars(", ")
            monthName(MonthNames.ENGLISH_ABBREVIATED) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
        }.format(this)
    } else {
        LocalDate.Format {
            // FIXME doesn't work with more than 2 MM
            //  byUnicodePattern("MMMM' 'dd', 'yyyy")
            monthName(MonthNames.ENGLISH_FULL) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
            chars(", ")
            year()
        }.format(this)
    }
}

@Composable
fun CompletedTaskRow(
    task: TaskUIModel,
    onAction: (TaskAction) -> Unit,
) {
    Row(
        Modifier
            .testTag(COMPLETED_TASK_ROW)
            .clickable(onClick = { onAction(TaskAction.Edit(task)) })
    ) {
        IconButton(onClick = { onAction(TaskAction.ToggleCompletion(task)) }, Modifier.testTag(COMPLETED_TASK_ICON)) {
            Icon(LucideIcons.CircleCheckBig, null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                task.title,
                textDecoration = TextDecoration.LineThrough,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.notes.isNotBlank()) {
                    Text(
                        task.notes,
                        modifier = Modifier.testTag(COMPLETED_TASK_NOTES),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
                if (task.completionDate != null) {
                    Text(
                        stringResource(Res.string.task_list_pane_completed_date_label, task.completionDate.toLabel()),
                        modifier = Modifier.testTag(COMPLETED_TASK_COMPLETION_DATE),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        IconButton(onClick = { onAction(TaskAction.Delete(task)) }, Modifier.testTag(COMPLETED_TASK_DELETE_ICON)) {
            Icon(LucideIcons.Trash, stringResource(Res.string.task_list_pane_delete_task_icon_content_desc))
        }
    }
}