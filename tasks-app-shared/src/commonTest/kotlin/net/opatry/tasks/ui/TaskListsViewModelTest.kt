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

package net.opatry.tasks.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.opatry.Logger
import net.opatry.tasks.app.ui.TaskEvent
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.model.DateRange
import net.opatry.tasks.app.ui.model.TaskId
import net.opatry.tasks.app.ui.model.TaskListId
import net.opatry.tasks.app.ui.model.TaskListUIModel
import net.opatry.tasks.app.ui.model.TaskUIModel
import net.opatry.tasks.data.TaskListSorting
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.test.MainDispatcherRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.BDDMockito.mock
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun buildMoments(dateStr: String = "2024-10-16"): Pair<LocalDate, Instant> {
    val date = LocalDate.parse(dateStr)
    val instant = LocalDateTime.parse("${date}T00:00:00").toInstant(TimeZone.currentSystemDefault())
    return date to instant
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class TaskListsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskListsFlow = MutableSharedFlow<List<TaskListDataModel>>()

    @Mock
    private lateinit var taskRepository: TaskRepository

    @Mock
    private lateinit var logger: Logger

    private lateinit var viewModel: TaskListsViewModel

    @BeforeTest
    fun setUp() {
        `when`(taskRepository.getTaskLists()).thenReturn(taskListsFlow)

        // ViewModel must be created **AFTER** `taskRepository.getTaskLists()` is mocked
        viewModel = TaskListsViewModel(logger, taskRepository)
    }

    @Test
    fun `collecting task lists should be notified of new task list properly ordered when created in repository`() = runTest {
        // FIXME extracting a mockable UiModel mapper would simplify test
        //  and delegate the complex mapping of task lists testing to the mapper
        //  see #116
        val collectedTaskLists = mutableListOf<List<TaskListUIModel>>()
        val collectorJob = launch(coroutineContext) {
            viewModel.taskLists.toList(collectedTaskLists)
        }
        advanceUntilIdle()

        val pastInstant = LocalDateTime.parse("1999-12-31T00:00:00").toInstant(TimeZone.currentSystemDefault())
        val futureInstant = LocalDateTime.parse("2999-12-31T00:00:00").toInstant(TimeZone.currentSystemDefault())
        val updateInstant = LocalDateTime.parse("2024-10-16T00:00:00").toInstant(TimeZone.currentSystemDefault())
        taskListsFlow.emit(
            listOf(
                TaskListDataModel(
                    id = 1,
                    title = "list1",
                    lastUpdate = updateInstant,
                    tasks = listOf(
                        TaskDataModel(
                            id = 101,
                            title = "task1",
                            notes = "notes1",
                            isCompleted = false,
                            dueDate = futureInstant,
                            lastUpdateDate = updateInstant,
                            completionDate = null,
                            position = "1",
                            indent = 0,
                        ),
                        TaskDataModel(
                            id = 102,
                            title = "task2",
                            notes = "notes2",
                            isCompleted = false,
                            dueDate = pastInstant,
                            lastUpdateDate = updateInstant,
                            completionDate = null,
                            position = "2",
                            indent = 0,
                        ),
                        TaskDataModel(
                            id = 103,
                            title = "task3",
                            notes = "notes3",
                            isCompleted = true,
                            dueDate = null,
                            lastUpdateDate = updateInstant,
                            completionDate = updateInstant,
                            position = "3",
                            indent = 0,
                        ),
                    ),
                    sorting = TaskListSorting.DueDate,
                )
            )
        )
        advanceUntilIdle()

        assertEquals(1, collectedTaskLists.size)
        val taskLists = collectedTaskLists.last()
        assertEquals(1, taskLists.size)
        val taskList = taskLists.last()
        assertEquals(TaskListId(1), taskList.id)
        assertEquals("list1", taskList.title)
        assertEquals(
            mapOf<DateRange?, List<TaskUIModel>>(
                DateRange.Overdue(date = LocalDate(1970, 1, 1), numberOfDays = -1) to listOf(
                    TaskUIModel(
                        id = TaskId(value = 102),
                        title = "task2",
                        dueDate = LocalDate(1999, 12, 31),
                        completionDate = null,
                        notes = "notes2",
                        isCompleted = false,
                        position = "2",
                        indent = 0
                    )
                ),
                DateRange.Later(date = LocalDate(2999, 12, 31), numberOfDays = 355983) to listOf(
                    TaskUIModel(
                        id = TaskId(value = 101),
                        title = "task1",
                        dueDate = LocalDate(2999, 12, 31),
                        completionDate = null,
                        notes = "notes1",
                        isCompleted = false,
                        position = "1",
                        indent = 0
                    )
                ),

                ),
            taskList.remainingTasks
        )
        assertEquals(
            listOf(
                TaskUIModel(
                    id = TaskId(value = 103),
                    title = "task3",
                    dueDate = null,
                    completionDate = null,
                    notes = "notes3",
                    isCompleted = true,
                    position = "3",
                    indent = 0
                )
            ),
            taskList.completedTasks
        )

        collectorJob.cancel()
    }

    @Test
    fun `createTaskList should update repository`() = runTest {
        viewModel.createTaskList("tasks")
        advanceUntilIdle()

        then(taskRepository).should().createTaskList("tasks")
    }

    @Test
    fun `createTaskList failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.createTaskList("tasks"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.createTaskList("tasks")
        advanceUntilIdle()

        then(logger).should().logError("Error while creating task list", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.Create, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `createTask should update repository`() = runTest {
        val taskListId = TaskListId(100)

        viewModel.createTask(taskListId, "task")
        advanceUntilIdle()

        then(taskRepository).should().createTask(100, "task")
    }

    @Test
    fun `createTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.createTask(100, "task"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.createTask(TaskListId(100), "task")
        advanceUntilIdle()

        then(logger).should().logError("Error while creating task (TaskListId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Create, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `createTask with extra parameters should update repository accordingly`() = runTest {
        val (date, instant) = buildMoments()

        viewModel.createTask(TaskListId(100), "task", "notes", date)
        advanceUntilIdle()

        then(taskRepository).should().createTask(100, "task", "notes", instant)
    }

    @Test
    fun `deleteTask should update repository`() = runTest {
        viewModel.deleteTask(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().deleteTask(100)
    }

    @Test
    fun `deleteTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.deleteTask(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.deleteTask(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while deleting task (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Delete, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `deleteTaskList should update repository`() = runTest {
        viewModel.deleteTaskList(TaskListId(1))
        advanceUntilIdle()

        then(taskRepository).should().deleteTaskList(1)
    }

    @Test
    fun `deleteTaskList failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.deleteTaskList(1))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.deleteTaskList(TaskListId(1))
        advanceUntilIdle()

        then(logger).should().logError("Error while deleting task list (TaskListId(value=1))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.Delete, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `renameTaskList should update repository`() = runTest {
        viewModel.renameTaskList(TaskListId(1), "newTitle")
        advanceUntilIdle()

        then(taskRepository).should().renameTaskList(1, "newTitle")
    }

    @Test
    fun `renameTaskList failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.renameTaskList(1, "newTitle"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.renameTaskList(TaskListId(1), "newTitle")
        advanceUntilIdle()

        then(logger).should().logError("Error while renaming task list (TaskListId(value=1))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.Rename, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `clearTaskListCompletedTasks should update repository`() = runTest {
        viewModel.clearTaskListCompletedTasks(TaskListId(1))
        advanceUntilIdle()

        then(taskRepository).should().clearTaskListCompletedTasks(1)
    }

    @Test
    fun `clearTaskListCompletedTasks failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.clearTaskListCompletedTasks(1))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.clearTaskListCompletedTasks(TaskListId(1))
        advanceUntilIdle()

        then(logger).should().logError("Error while clearing completed tasks (TaskListId(value=1))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.ClearCompletedTasks, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `sortBy(DueDate) should update repository by DueDate`() = runTest {
        val sorting = TaskListSorting.DueDate

        viewModel.sortBy(TaskListId(1), sorting)
        advanceUntilIdle()

        then(taskRepository).should().sortTasksBy(1, sorting)
    }

    @Test
    fun `sortBy(DueDate) failure when calling repository should log error`() = runTest {
        val sorting = TaskListSorting.DueDate
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.sortTasksBy(1, sorting))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.sortBy(TaskListId(1), sorting)
        advanceUntilIdle()

        then(logger).should().logError("Error while sorting task list (TaskListId(value=1)) by DueDate", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.Sort, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `sortBy(Manual) should update repository by DueDate`() = runTest {
        val sorting = TaskListSorting.Manual

        viewModel.sortBy(TaskListId(1), sorting)
        advanceUntilIdle()

        then(taskRepository).should().sortTasksBy(1, sorting)
    }

    @Test
    fun `sortBy(Manual) failure when calling repository should log error`() = runTest {
        val sorting = TaskListSorting.Manual
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.sortTasksBy(1, sorting))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.sortBy(TaskListId(1), sorting)
        advanceUntilIdle()

        then(logger).should().logError("Error while sorting task list (TaskListId(value=1)) by Manual", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.TaskList.Sort, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `toggleTaskCompletionState should update repository`() = runTest {
        viewModel.toggleTaskCompletionState(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().toggleTaskCompletionState(100)
    }

    @Test
    fun `toggleTaskCompletionState failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.toggleTaskCompletionState(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.toggleTaskCompletionState(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while toggling task completion state (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.ToggleCompletionState, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTask should update repository`() = runTest {
        viewModel.updateTask(TaskListId(1), TaskId(100), "title2", "", null)
        advanceUntilIdle()

        then(taskRepository).should().updateTask(1, 100, "title2", "", null)
    }

    @Test
    fun `updateTask with extra parameters should update repository accordingly`() = runTest {
        val (date, instant) = buildMoments()
        viewModel.updateTask(TaskListId(1), TaskId(100), "title2", "notes2", date)
        advanceUntilIdle()

        then(taskRepository).should().updateTask(1, 100, "title2", "notes2", instant)
    }

    @Test
    fun `updateTask with dirty string parameters should update repository with strings cleaned`() = runTest {
        viewModel.updateTask(TaskListId(1), TaskId(100), "    title2    ", "    notes2    ", null)
        advanceUntilIdle()

        then(taskRepository).should().updateTask(1, 100, "title2", "notes2", null)
    }

    @Test
    fun `updateTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.updateTask(1, 100, "", "", null))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTask(TaskListId(1), TaskId(100), "", "", null)
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTaskTitle should update repository`() = runTest {
        viewModel.updateTaskTitle(TaskId(100), "title2")
        advanceUntilIdle()

        then(taskRepository).should().updateTaskTitle(100, "title2")
    }

    @Test
    fun `updateTaskTitle failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.updateTaskTitle(100, "title2"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTaskTitle(TaskId(100), "title2")
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task title (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTaskNotes should update repository`() = runTest {
        viewModel.updateTaskNotes(TaskId(100), "notes2")
        advanceUntilIdle()

        then(taskRepository).should().updateTaskNotes(100, "notes2")
    }

    @Test
    fun `updateTaskNotes failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.updateTaskNotes(100, "notes2"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTaskNotes(TaskId(100), "notes2")
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task notes (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTaskDueDate should update repository`() = runTest {
        val (dueDate, instant) = buildMoments()
        viewModel.updateTaskDueDate(TaskId(100), dueDate)
        advanceUntilIdle()

        then(taskRepository).should().updateTaskDueDate(100, instant)
    }

    @Test
    fun `updateTaskDueDate failure when calling repository should log error`() = runTest {
        val (dueDate, instant) = buildMoments()
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.updateTaskDueDate(100, instant))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTaskDueDate(TaskId(100), dueDate)
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task due date (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `unindentTask should update repository`() = runTest {
        viewModel.unindentTask(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().unindentTask(100)
    }

    @Test
    fun `unindentTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.unindentTask(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.unindentTask(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while unindenting task (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Unindent, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `indentTask should update repository`() = runTest {
        viewModel.indentTask(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().indentTask(100)
    }

    @Test
    fun `indentTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.indentTask(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.indentTask(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while indenting task (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Indent, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `moveToTop should update repository`() = runTest {
        viewModel.moveToTop(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().moveToTop(100)
    }

    @Test
    fun `moveToTop failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.moveToTop(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.moveToTop(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while moving task to top (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Move, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `moveToList should update repository`() = runTest {
        viewModel.moveToList(TaskId(100), TaskListId(3))
        advanceUntilIdle()

        then(taskRepository).should().moveToList(100, 3)
    }

    @Test
    fun `moveToList failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.moveToList(100, 3))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.moveToList(TaskId(100), TaskListId(3))
        advanceUntilIdle()

        then(logger).should().logError("Error while moving task to list (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Move, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `moveToNewList should update repository`() = runTest {
        viewModel.moveToNewList(TaskId(100), "newList")
        advanceUntilIdle()

        then(taskRepository).should().moveToNewList(100, "newList")
    }

    @Test
    fun `moveToNewList failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.moveToNewList(100, "newList"))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.moveToNewList(TaskId(100), "newList")
        advanceUntilIdle()

        then(logger).should().logError("Error while moving task to new list (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Move, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `restoreTask should update repository`() = runTest {
        viewModel.restoreTask(TaskId(100))
        advanceUntilIdle()

        then(taskRepository).should().restoreTask(100)
    }

    @Test
    fun `restoreTask failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.restoreTask(100))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.restoreTask(TaskId(100))
        advanceUntilIdle()

        then(logger).should().logError("Error while restoring task (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Restore, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTaskDueDate with null should update repository`() = runTest {
        viewModel.updateTaskDueDate(TaskId(100), null)
        advanceUntilIdle()

        then(taskRepository).should().updateTaskDueDate(100, null)
    }

    @Test
    fun `updateTaskDueDate with null failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        `when`(taskRepository.updateTaskDueDate(100, null))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTaskDueDate(TaskId(100), null)
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task due date (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }

    @Test
    fun `updateTaskDueDate with date should update repository with instant`() = runTest {
        val (dueDate, instant) = buildMoments()

        viewModel.updateTaskDueDate(TaskId(100), dueDate)
        advanceUntilIdle()

        then(taskRepository).should().updateTaskDueDate(100, instant)
    }

    @Test
    fun `updateTaskDueDate with date failure when calling repository should log error`() = runTest {
        val e = mock(RuntimeException::class.java)
        val (dueDate, instant) = buildMoments()
        `when`(taskRepository.updateTaskDueDate(100, instant))
            .thenThrow(e)

        val events = mutableListOf<TaskEvent>()
        val eventCollectorJob = launch {
            viewModel.eventFlow.toList(events)
        }

        viewModel.updateTaskDueDate(TaskId(100), dueDate)
        advanceUntilIdle()

        then(logger).should().logError("Error while updating task due date (TaskId(value=100))", e)
        assertEquals(1, events.size)
        assertEquals(TaskEvent.Error.Task.Update, events.first())

        eventCollectorJob.cancel()
    }
}