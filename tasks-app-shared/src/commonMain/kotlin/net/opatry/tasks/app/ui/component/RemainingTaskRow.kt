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

import CalendarPlus
import CalendarX2
import Circle
import EllipsisVertical
import LucideIcons
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskListUIModel
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.MENU_ICON
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.NOTES
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.RESET_DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.ROW
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_DUE_DATE_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_DUE_DATE_TOMORROW_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.SET_TASK_DUE_DATE_TODAY_CHIP
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.TOGGLE_ICON
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_days_ago
import net.opatry.tasks.resources.task_due_date_label_no_date
import net.opatry.tasks.resources.task_due_date_label_past
import net.opatry.tasks.resources.task_due_date_label_today
import net.opatry.tasks.resources.task_due_date_label_tomorrow
import net.opatry.tasks.resources.task_due_date_label_weeks_ago
import net.opatry.tasks.resources.task_due_date_label_yesterday
import net.opatry.tasks.resources.task_due_date_reset
import net.opatry.tasks.resources.task_due_date_set
import net.opatry.tasks.resources.task_list_pane_task_options_icon_content_desc
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import kotlin.math.abs

@VisibleForTesting
object RemainingTaskRowTestTag {
    const val ROW = "REMAINING_TASK_ROW"
    const val TOGGLE_ICON = "REMAINING_TASK_TOGGLE_ICON"
    const val MENU_ICON = "REMAINING_TASK_MENU_ICON"
    const val NOTES = "REMAINING_TASK_NOTES"
    const val DUE_DATE_CHIP = "REMAINING_TASK_DUE_DATE_CHIP"
    const val RESET_DUE_DATE_CHIP = "REMAINING_TASK_RESET_DUE_DATE_CHIP"
    const val SET_TASK_DUE_DATE_TODAY_CHIP = "REMAINING_TASK_SET_TASK_DUE_DATE_TODAY_CHIP"
    const val SET_DUE_DATE_TOMORROW_CHIP = "REMAINING_TASK_SET_DUE_DATE_TOMORROW_CHIP"
    const val SET_DUE_DATE_CHIP = "REMAINING_TASK_SET_DUE_DATE_CHIP"
}

@VisibleForTesting
@Composable
internal fun DateRange?.toColor(): Color = when (this) {
    is DateRange.Overdue -> MaterialTheme.colorScheme.error
    is DateRange.Today -> MaterialTheme.colorScheme.primary
    is DateRange.Later,
    DateRange.None,
    null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
}

@VisibleForTesting
@Composable
internal fun DateRange.toLabel(sectionLabel: Boolean = false): String = when (this) {
    is DateRange.Overdue -> {
        val numberOfDays = abs(numberOfDays)
        when {
            sectionLabel -> stringResource(Res.string.task_due_date_label_past)
            numberOfDays == 1 -> stringResource(Res.string.task_due_date_label_yesterday)
            numberOfDays < 7 -> pluralStringResource(Res.plurals.task_due_date_label_days_ago, numberOfDays, numberOfDays)
            else -> (numberOfDays / 7).let { numberOfWeeks ->
                pluralStringResource(Res.plurals.task_due_date_label_weeks_ago, numberOfWeeks, numberOfWeeks)
            }
        }
    }

    is DateRange.Today -> stringResource(Res.string.task_due_date_label_today)
    is DateRange.Later -> when {
        numberOfDays == 1 -> stringResource(Res.string.task_due_date_label_tomorrow)
        // TODO localize names & format
        date.year == Clock.System.todayIn(TimeZone.UTC).year -> LocalDate.Format {
            // FIXME doesn't work with more than 2 dd or MM
            //  byUnicodePattern("ddd', 'MMM' 'yyyy")
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // TODO translation
            chars(", ")
            monthName(MonthNames.ENGLISH_ABBREVIATED) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
        }.format(date)

        else -> LocalDate.Format {
            // FIXME doesn't work with more than 2 MM
            //  byUnicodePattern("MMMM' 'dd', 'yyyy")
            monthName(MonthNames.ENGLISH_FULL) // TODO translation
            char(' ')
            dayOfMonth(Padding.NONE)
            chars(", ")
            year()
        }.format(date)
    }

    DateRange.None -> when {
        sectionLabel -> stringResource(Res.string.task_due_date_label_no_date)
        else -> ""
    }
}

@Composable
fun RemainingTaskRow(
    taskLists: List<TaskListUIModel>,
    task: TaskUIModel.Todo,
    showDate: Boolean = true,
    onAction: (TaskAction) -> Unit,
) {
    var showContextualMenu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .testTag(ROW)
            .clickable(onClick = { onAction(TaskAction.Edit(task)) })
    ) {
        IconButton(
            onClick = { onAction(TaskAction.ToggleCompletion(task)) },
            modifier = Modifier
                .testTag(TOGGLE_ICON)
                .padding(start = 36.dp * task.indent)
        ) {
            Icon(LucideIcons.Circle, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )

            if (task.notes.isNotBlank()) {
                Text(
                    task.notes,
                    modifier = Modifier.testTag(NOTES),
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            if (showDate && task.dueDate != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onAction(TaskAction.UpdateDueDate(task, DueDateUpdate.Pick)) },
                        label = {
                            Text(task.dateRange.toLabel())
                        },
                        modifier = Modifier.testTag(DUE_DATE_CHIP),
                        shape = MaterialTheme.shapes.large,
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = task.dateRange.toColor(),
                        ),
                    )
                    AssistChip(
                        onClick = { onAction(TaskAction.UpdateDueDate(task, DueDateUpdate.Reset)) },
                        label = {
                            Icon(
                                LucideIcons.CalendarX2,
                                stringResource(Res.string.task_due_date_reset),
                                Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.testTag(RESET_DUE_DATE_CHIP),
                        shape = MaterialTheme.shapes.large,
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            } else if (showDate) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    AssistChip(
                        onClick = { onAction(TaskAction.UpdateDueDate(task, DueDateUpdate.Today)) },
                        label = {
                            Text(stringResource(Res.string.task_due_date_label_today))
                        },
                        modifier = Modifier.testTag(SET_TASK_DUE_DATE_TODAY_CHIP),
                        shape = MaterialTheme.shapes.large,
                        colors = colors,
                    )
                    AssistChip(
                        onClick = { onAction(TaskAction.UpdateDueDate(task, DueDateUpdate.Tomorrow)) },
                        label = {
                            Text(stringResource(Res.string.task_due_date_label_tomorrow))
                        },
                        modifier = Modifier.testTag(SET_DUE_DATE_TOMORROW_CHIP),
                        shape = MaterialTheme.shapes.large,
                        colors = colors,
                    )
                    AssistChip(
                        onClick = { onAction(TaskAction.UpdateDueDate(task, DueDateUpdate.Pick)) },
                        label = {
                            Icon(
                                LucideIcons.CalendarPlus,
                                stringResource(Res.string.task_due_date_set),
                                Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.testTag(SET_DUE_DATE_CHIP),
                        shape = MaterialTheme.shapes.large,
                        colors = colors,
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { showContextualMenu = true }, Modifier.testTag(MENU_ICON)) {
                Icon(LucideIcons.EllipsisVertical, stringResource(Res.string.task_list_pane_task_options_icon_content_desc))
            }
            TaskMenu(taskLists, task, showContextualMenu) { action ->
                showContextualMenu = false
                action?.let(onAction)
            }
        }
    }
}

private class RemainingTaskRowPreviewDataProvider :
    PreviewParameterProvider<TaskUIModel.Todo> {
    override val values = sequenceOf(
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "Without due date",
            dueDate = null,
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With notes",
            notes = "Some notes\nthat are long enough\nto be wrapped on multiple lines",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With very very very very very very very very long title",
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date in the past",
            dueDate = Clock.System.todayIn(TimeZone.UTC).minus(3, DateTimeUnit.MONTH),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date today",
            dueDate = Clock.System.todayIn(TimeZone.UTC),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date yesterday",
            dueDate = Clock.System.todayIn(TimeZone.UTC).minus(1, DateTimeUnit.DAY),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date tomorrow",
            dueDate = Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.DAY),
        ),
        TaskUIModel.Todo(
            id = TaskId(0),
            title = "With due date in distant future",
            dueDate = Clock.System.todayIn(TimeZone.UTC).plus(1, DateTimeUnit.MONTH),
        ),
    )
}

@Preview
@Composable
private fun CompletedTaskRowPreviewPreview(
    @PreviewParameter(RemainingTaskRowPreviewDataProvider::class)
    task: TaskUIModel.Todo,
) {
    TaskfolioThemedPreview {
        RemainingTaskRow(
            taskLists = emptyList(),
            task = task,
            showDate = true,
            onAction = {}
        )
    }
}
