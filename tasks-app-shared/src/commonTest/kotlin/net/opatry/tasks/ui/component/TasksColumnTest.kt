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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.opatry.tasks.app.presentation.model.DateRange
import net.opatry.tasks.app.presentation.model.TaskId
import net.opatry.tasks.app.presentation.model.TaskUIModel
import net.opatry.tasks.app.ui.component.CompletedTaskRowTestTag.COMPLETED_TASK_ROW
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_DELETE_BUTTON
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_EMPTY_STATE
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.BROKEN_LIST_REPAIR_BUTTON
import net.opatry.tasks.app.ui.component.EmptyStatesTestTag.NO_TASKS_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumn
import net.opatry.tasks.app.ui.component.TasksColumnTestTag
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.ALL_COMPLETE_EMPTY_STATE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE_LABEL
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.TASKS_COLUMN
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.task_due_date_label_no_date
import net.opatry.tasks.resources.task_due_date_label_past
import net.opatry.tasks.resources.task_due_date_label_today
import net.opatry.tasks.resources.task_due_date_label_tomorrow
import net.opatry.tasks.resources.task_list_pane_completed_section_title_with_count
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import net.opatry.tasks.app.ui.component.RemainingTaskRowTestTag.ROW as REMAINING_TASK_ROW

@Suppress("TestFunctionName")
@OptIn(ExperimentalTestApi::class)
class TasksColumnTest {
    private val today: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

    @Test
    fun TasksColumn_TaskRows() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 2, completedTaskCount = 1)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onAllNodesWithTag(REMAINING_TASK_ROW)
            .assertCountEquals(2)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
    }

    @Test
    fun TasksColumn_FullyEmptyState() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0, completedTaskCount = 0)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(NO_TASKS_EMPTY_STATE)
            .assertIsDisplayed()

        onNodeWithTag(TASKS_COLUMN)
            .assertDoesNotExist()
    }

    @Test
    fun TasksColumn_BrokenListEmptyState() = runComposeUiTest {
        val taskList = createTaskList().copy(
            remainingTasks = mapOf(
                null to listOf(
                    TaskUIModel.Todo(
                        id = TaskId(1),
                        title = "broken indent",
                        indent = 42,
                    )
                )
            ),
        )

        var deleteCalled = false
        var repairCalled = false
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
                onDeleteList = {
                    deleteCalled = true
                },
                onRepairList = {
                    repairCalled = true
                }
            )
        }

        onNodeWithTag(BROKEN_LIST_EMPTY_STATE)
            .assertIsDisplayed()

        onNodeWithTag(BROKEN_LIST_DELETE_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertTrue(deleteCalled, "Delete callback should have been called").also {
            deleteCalled = false
        }

        // TODO check behavior once repair is implemented
        onNodeWithTag(BROKEN_LIST_REPAIR_BUTTON)
            .assertIsDisplayed()
            .assertIsNotEnabled()
//            .assertIsEnabled()
//            .performClick()
//
//        assertTrue(repairCalled, "Repair callback should have been called").also {
//            repairCalled = false
//        }
    }

    @Test
    fun TasksColumn_AllCompletedEmptyState() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 0, completedTaskCount = 1)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(NO_TASKS_EMPTY_STATE)
            .assertDoesNotExist()

        onNodeWithTag(ALL_COMPLETE_EMPTY_STATE)
            .assertIsDisplayed()

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
    }

    @Test
    fun TasksColumn_NoCompletedSectionIfNotNeeded() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 2, completedTaskCount = 0)
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertDoesNotExist()

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)
    }

    @Test
    fun TasksColumn_ToggleCompletedSection() = runComposeUiTest {
        val taskList = createTaskList(remainingTaskCount = 1, completedTaskCount = 2)
        lateinit var completedTaskLabel: String
        setContent {
            completedTaskLabel = stringResource(Res.string.task_list_pane_completed_section_title_with_count, 2)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
            .performClick()
            .assertIsDisplayed()

        // FIXME why useUnmergedTree is needed?
        onNodeWithTag(COMPLETED_TASKS_TOGGLE_LABEL, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals(completedTaskLabel)

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(2)

        onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .performClick()
            .assertIsDisplayed()

        onAllNodesWithTag(COMPLETED_TASK_ROW)
            .assertCountEquals(0)
    }

    @Test
    fun TasksColumn_WithDateRange_None() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.None to listOf(createTask()),
                )
            )
        lateinit var noDateStr: String
        setContent {
            noDateStr = stringResource(Res.string.task_due_date_label_no_date)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "none")
            .assertIsDisplayed()
            .assertTextEquals(noDateStr)
    }

    @Test
    fun TasksColumn_WithDateRange_WeekOld() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Overdue(today.minus(4, DateTimeUnit.WEEK), -28) to listOf(createTask()),
                )
            )
        lateinit var pastStr: String
        setContent {
            pastStr = stringResource(Res.string.task_due_date_label_past)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "overdue-28")
            .assertIsDisplayed()
            .assertTextEquals(pastStr)
    }

    @Test
    fun TasksColumn_WithDateRange_ThisWeek() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Overdue(today.minus(4, DateTimeUnit.DAY), -4) to listOf(createTask()),
                )
            )
        lateinit var pastStr: String
        setContent {
            pastStr = stringResource(Res.string.task_due_date_label_past)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "overdue-4")
            .assertIsDisplayed()
            .assertTextEquals(pastStr)
    }

    @Test
    fun TasksColumn_WithDateRange_Yesterday() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Overdue(today.minus(1, DateTimeUnit.DAY), -1) to listOf(createTask()),
                )
            )
        lateinit var pastStr: String
        setContent {
            pastStr = stringResource(Res.string.task_due_date_label_past)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "overdue-1")
            .assertIsDisplayed()
            .assertTextEquals(pastStr)
    }

    @Test
    fun TasksColumn_WithDateRange_Today() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Today(today) to listOf(createTask()),
                )
            )
        lateinit var todayStr: String
        setContent {
            todayStr = stringResource(Res.string.task_due_date_label_today)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "today")
            .assertIsDisplayed()
            .assertTextEquals(todayStr)
    }

    @Test
    fun TasksColumn_WithDateRange_Tomorrow() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Later(today.plus(1, DateTimeUnit.DAY), 1) to listOf(createTask()),
                )
            )
        lateinit var tomorrowStr: String
        setContent {
            tomorrowStr = stringResource(Res.string.task_due_date_label_tomorrow)
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "later1")
            .assertIsDisplayed()
            .assertTextEquals(tomorrowStr)
    }

    @Ignore("TODO complicated to test relative date range in test and missing localization")
    @Test
    fun TasksColumn_WithDateRange_ThisMonth() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Later(today.plus(1, DateTimeUnit.MONTH), 30) to listOf(createTask()),
                )
            )
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "later30")
            .assertIsDisplayed()
        // TODO complicated to do in test
        //  .assertTextEquals(tomorrowStr)
    }

    @Test
    fun TasksColumn_WithDateRange_InManyYears() = runComposeUiTest {
        val taskList = createTaskList()
            .copy(
                remainingTasks = mapOf(
                    DateRange.Later(LocalDate(2500, 1, 1), 1000) to listOf(createTask()),
                )
            )
        setContent {
            TasksColumn(
                taskLists = listOf(taskList),
                selectedTaskList = taskList,
            )
        }

        onNodeWithTag(TasksColumnTestTag.DATE_RANGE_STICKY_HEADER + "later1000")
            .assertIsDisplayed()
            .assertTextEquals("January 1, 2500")
    }
}