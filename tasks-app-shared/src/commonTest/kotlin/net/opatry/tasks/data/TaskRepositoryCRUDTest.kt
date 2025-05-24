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

package net.opatry.tasks.data

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.tasks.data.util.runTaskRepositoryTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

private suspend fun TaskRepository.createAndGetTaskList(title: String): TaskListDataModel {
    createTaskList(title)
    return getTaskLists().firstOrNull()?.maxByOrNull(TaskListDataModel::id)?.takeIf { it.title == title }
        ?: error("Task list not found")
}

private suspend fun TaskRepository.createAndGetTask(taskListId: Long, taskTitle: String): TaskDataModel {
    createTask(taskListId, null, taskTitle)
    return findTaskListById(taskListId)
        ?.tasks
        ?.maxByOrNull(TaskDataModel::id)
        ?.takeIf { it.title == taskTitle }
        ?: error("Task not found")
}

private suspend fun TaskRepository.createAndGetTask(taskListTitle: String, taskTitle: String): Pair<TaskListDataModel, TaskDataModel> {
    val taskList = createAndGetTaskList(taskListTitle)
    return taskList to createAndGetTask(taskList.id, taskTitle)
}

private suspend fun TaskRepository.findTaskListById(id: Long): TaskListDataModel? {
    return getTaskLists().firstOrNull()?.firstOrNull { it.id == id }
}

class TaskRepositoryCRUDTest {

    @Test
    fun `create task list`() = runTaskRepositoryTest { repository ->
        val taskListId = repository.createTaskList("My tasks")

        val taskLists = repository.getTaskLists().firstOrNull()
        assertNotNull(taskLists)
        assertEquals(1, taskLists.size)
        assertEquals("My tasks", taskLists.first().title)
        assertEquals(taskListId, taskLists.first().id)
    }

    @Test
    fun `rename task list`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.renameTaskList(taskList.id, "My renamed list")

        val taskListRenamed = repository.findTaskListById(taskList.id)
        assertNotNull(taskListRenamed)
        assertEquals("My renamed list", taskListRenamed.title, "Updated name is invalid")
    }

    @Test
    fun `rename unavailable task list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task list id 42") {
            repository.renameTaskList(42L, "toto")
        }
    }

    @Test
    fun `delete task list`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.deleteTaskList(taskList.id)

        val taskLists = repository.getTaskLists().firstOrNull()
        assertNotNull(taskLists)
        assertEquals(0, taskLists.size, "No task list expected")
    }

    @Test
    fun `delete unavailable task list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task list id 42") {
            repository.deleteTaskList(42L)
        }
    }

    @Test
    fun `create task`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        val taskId = repository.createTask(taskList.id, null, "My task")

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("My task", tasks.first().title)
        assertFalse(tasks.first().isCompleted)
        assertEquals(taskId, tasks.first().id)
    }

    @Test
    fun `create 2 tasks puts the second at start`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.createTask(taskList.id, null, "task1")
        repository.createTask(taskList.id, null, "task2")

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(2, tasks.size)
        val firstTask = tasks.find { it.position == "00000000000000000000" }
        assertNotNull(firstTask, "task with position 0 not found")
        val lastTask = tasks.find { it.position == "00000000000000000001" }
        assertNotNull(lastTask, "task with position 1 not found")
        assertEquals("task2", firstTask.title, "first task should be last created")
        assertEquals("task1", lastTask.title, "last task should be first created")
    }

    @Test
    fun `create task in an unavailable task list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task list id 42") {
            repository.createTask(42L, null, "toto")
        }
    }

    @Test
    fun `create task with an unavailable parent task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")
        assertFailsWith<IllegalArgumentException>("Invalid parent task id 42") {
            repository.createTask(taskList.id, 42L, "toto")
        }
    }

    @Test
    fun `rename task`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskTitle(task.id, "My renamed task")

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("My renamed task", tasks.first().title)
    }

    @Test
    fun `rename unavailable task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.updateTaskTitle(42L, "toto")
        }
    }

    @Test
    fun `edit task with all parameters`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        val updatedDate = Instant.DISTANT_FUTURE
        repository.updateTask(task.id, "new title", "new notes", updatedDate)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("new title", tasks.first().title)
        assertEquals("new notes", tasks.first().notes)
        assertEquals(updatedDate, tasks.first().dueDate)
    }

    @Test
    fun `edit unavailable task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.updateTask(42L, "toto", "titi", null)
        }
    }

    @Test
    fun `edit task notes`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskNotes(task.id, "These are some notes")

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("These are some notes", tasks.first().notes)
    }

    @Test
    fun `edit unavailable task notes should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.updateTaskNotes(42L, "toto")
        }
    }

    @Test
    fun `edit task due date`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        val updatedDate = Instant.DISTANT_FUTURE
        repository.updateTaskDueDate(task.id, updatedDate)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals(updatedDate, tasks.first().dueDate)
    }

    @Test
    fun `reset task due date`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskDueDate(task.id, null)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals(null, tasks.first().dueDate)
    }

    @Test
    fun `edit unavailable task due date should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.updateTaskDueDate(42L, null)
        }
    }

    @Test
    fun `complete task`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.toggleTaskCompletionState(task.id)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertTrue(tasks.first().isCompleted)
    }

    @Test
    fun `toggle unavailable task completion state should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.toggleTaskCompletionState(42L)
        }
    }

    @Test
    fun `delete task`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.deleteTask(task.id)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(0, tasks.size, "Task should have been deleted")
    }

    @Test
    fun `delete unavailable task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.deleteTask(42L)
        }
    }

    @Test
    fun `delete task should recompute remaining tasks positions`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("My tasks", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")

        repository.deleteTask(task2.id)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size, "Task should have been deleted")
        assertEquals(task1.id, tasks.first().id)
        assertEquals("00000000000000000000", tasks.first().position, "Task position should have been updated")
    }

    @Test
    fun `move task to top`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("tasks", "t1")
        val task2 = repository.createAndGetTask(taskList.id, "t2")

        repository.moveToTop(task1.id)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(2, tasks.size)
        assertEquals(task1.id, tasks[0].id, "last task should now be first")
        assertEquals("00000000000000000000", tasks[0].position, "position should reflect new order")
        assertEquals(task2.id, tasks[1].id, "first task should now be last")
        assertEquals("00000000000000000001", tasks[1].position, "position should reflect new order")
    }

    @Test
    fun `move unavailable task to top should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.moveToTop(42L)
        }
    }

    @Test
    fun `move completed task to top should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (_, task1) = repository.createAndGetTask("tasks", "t1")
        repository.toggleTaskCompletionState(task1.id)

        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.moveToTop(task1.id)
        }
    }

    @Test
    fun `move task to list`() = runTaskRepositoryTest { repository ->
        val (taskList1, task1) = repository.createAndGetTask("list1", "t1")
        val task2 = repository.createAndGetTask(taskList1.id, "t2")
        val task3 = repository.createAndGetTask(taskList1.id, "t3")
        val (taskList2, task4) = repository.createAndGetTask("list2", "t4")

        repository.moveToList(task2.id, taskList2.id)

        val updatedTaskList1 = repository.findTaskListById(taskList1.id)
        assertNotNull(updatedTaskList1)
        assertEquals(2, updatedTaskList1.tasks.size)
        assertEquals(task3.id, updatedTaskList1.tasks[0].id)
        assertEquals("00000000000000000000", updatedTaskList1.tasks[0].position, "task from first list should have their position updated")
        assertEquals(task1.id, updatedTaskList1.tasks[1].id)
        assertEquals("00000000000000000001", updatedTaskList1.tasks[1].position, "task from first list should have their position updated")

        val updatedTaskList2 = repository.findTaskListById(taskList2.id)
        assertNotNull(updatedTaskList2)
        assertEquals(2, updatedTaskList2.tasks.size)
        assertEquals(task2.id, updatedTaskList2.tasks[0].id)
        assertEquals("00000000000000000000", updatedTaskList2.tasks[0].position, "task should be moved at first position")
        assertEquals(task4.id, updatedTaskList2.tasks[1].id)
        assertEquals("00000000000000000001", updatedTaskList2.tasks[1].position, "task from second list should have their position updated")
    }

    @Test
    fun `move unavailable task to list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("list1")
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.moveToList(42L, taskList.id)
        }
    }

    @Test
    fun `move task to unavailable list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("list1", "task1")
        assertFailsWith<IllegalArgumentException>("Invalid task list id 42") {
            repository.moveToList(task.id, 42L)
        }
    }

    @Test
    fun `move task to new list`() = runTaskRepositoryTest { repository ->
        val (taskList1, task1) = repository.createAndGetTask("list1", "t1")
        val task2 = repository.createAndGetTask(taskList1.id, "t2")

        val taskListId2 = repository.moveToNewList(task2.id, "list2")

        val updatedTaskList1 = repository.findTaskListById(taskList1.id)

        assertNotNull(updatedTaskList1)
        assertEquals(1, updatedTaskList1.tasks.size)
        assertEquals(task1.id, updatedTaskList1.tasks.first().id)
        assertEquals("00000000000000000000", updatedTaskList1.tasks.first().position, "task should be moved at first position")

        val taskList2 = repository.findTaskListById(taskListId2)
        assertNotNull(taskList2)
        assertEquals(1, taskList2.tasks.size)
        val updatedTask = taskList2.tasks.first()
        assertEquals(task2.id, updatedTask.id)
        assertEquals("00000000000000000000", updatedTask.position, "task should be moved at first position")
    }

    @Test
    fun `move unavailable task to new list should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.moveToNewList(42L, "toto")
        }
    }

    @Test
    fun `sorting tasks by title should honor title (ignore case) and ignore parent task link and indentation`() =
        runTaskRepositoryTest { repository ->
            val (taskList, task1) = repository.createAndGetTask("list", "t1")
            val task2 = repository.createAndGetTask(taskList.id, "T1")
            val task3 = repository.createAndGetTask(taskList.id, "t100")
            val task4 = repository.createAndGetTask(taskList.id, "t2")
            repository.indentTask(task3.id)
            // list
            //   - t2 [0000]
            //      - t100 [0000]
            //   - T1 [0001]
            //   - t1 [0002]

            repository.sortTasksBy(taskList.id, TaskListSorting.Title)

            // list
            //   - t1 (lowercase comes first)
            //   - T1 (uppercase come second)
            //   - t100 (no collator, 100 comes before 2)
            //   - t2
            val updatedTaskList = repository.findTaskListById(taskList.id)
            assertNotNull(updatedTaskList)
            val tasks = updatedTaskList.tasks
            assertEquals(4, tasks.size, "should have 4 tasks in list")
            assertEquals(task1.id, tasks[0].id, "first task should be t1")
            assertEquals(0, tasks[0].indent, "task shouldn't be indented in due date sorting")
            assertEquals("00000000000000000002", tasks[0].position, "task position should remain unchanged in due date sorting")
            assertEquals(task2.id, tasks[1].id, "second task should be T1")
            assertEquals(0, tasks[1].indent, "task shouldn't be indented in due date sorting")
            assertEquals("00000000000000000001", tasks[1].position, "task position should remain unchanged in due date sorting")
            assertEquals(task3.id, tasks[2].id, "third task should be t100")
            assertEquals(0, tasks[2].indent, "task shouldn't be indented in due date sorting")
            assertEquals("00000000000000000000", tasks[2].position, "task position should remain unchanged in due date sorting")
            assertEquals(task4.id, tasks[3].id, "fourth task should be t2")
            assertEquals(0, tasks[3].indent, "task shouldn't be indented in due date sorting")
            assertEquals("00000000000000000000", tasks[3].position, "task position should remain unchanged in due date sorting")
        }

    @Test
    fun `sorting tasks by due date should put no due date last`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val t1 = Clock.System.now() + 1.days
        repository.updateTaskDueDate(task1.id, t1)
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        repository.updateTaskDueDate(task2.id, null)

        repository.sortTasksBy(taskList.id, TaskListSorting.DueDate)

        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(task1.id, tasks[0].id, "task with due date should come first")
        assertEquals(t1, tasks[0].dueDate)
        assertEquals(task2.id, tasks[1].id, "task without due date should come last")
        assertNull(tasks[1].dueDate)
    }

    @Test
    fun `indent a task should keep its attributes unchanged`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val now = Clock.System.now()
        repository.updateTask(task1.id, "updateTitle1", "updatedNotes1", now)
        repository.createAndGetTask(taskList.id, "task2")

        repository.indentTask(task1.id)

        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(2, tasks.size)
        assertEquals(task1.id, tasks[1].id)
        assertEquals("updateTitle1", tasks[1].title)
        assertEquals("updatedNotes1", tasks[1].notes)
        assertEquals(now, tasks[1].dueDate)
        assertEquals(1, tasks[1].indent)
        assertEquals("00000000000000000000", tasks[1].position)
    }

    @Test
    fun `indent top level task should use previous sibling as parent task`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")

        repository.indentTask(task1.id)

        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(2, tasks.size)
        assertEquals(task2.id, tasks[0].id)
        assertEquals(0, tasks[0].indent)
        assertEquals("00000000000000000000", tasks[0].position)
        assertEquals(task1.id, tasks[1].id)
        assertEquals(1, tasks[1].indent)
        assertEquals("00000000000000000000", tasks[1].position)
    }

    @Test
    fun `indent first top level task after subtask should use subtask as previous sibling`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        val task3 = repository.createAndGetTask(taskList.id, "task3")
        val task4 = repository.createAndGetTask(taskList.id, "task4")
        repository.indentTask(task3.id)
        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //   - task2 [0001]
        //   - task1 [0002]

        repository.indentTask(task2.id)

        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //      - task2 [0001] * updated indentation & position (even if identical here)
        //   - task1 [0001] * updated position
        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(4, tasks.size, "should have 4 tasks in list")
        assertEquals(task4.id, tasks[0].id, "first task should be task4")
        assertEquals(0, tasks[0].indent, "first task shouldn't be indented")
        assertEquals("00000000000000000000", tasks[0].position, "first task should be at position 0 (first task)")
        assertEquals(task3.id, tasks[1].id, "second task should be task3")
        assertEquals(1, tasks[1].indent, "second task should be a subtask indented by 1")
        assertEquals("00000000000000000000", tasks[1].position, "second task should be at position 0 (first subtask)")
        assertEquals(task2.id, tasks[2].id, "third task should be task2")
        assertEquals(1, tasks[2].indent, "third task should be a subtask indented by 1")
        assertEquals("00000000000000000001", tasks[2].position, "third task should be at position 1 (second subtask)")
        assertEquals(task1.id, tasks[3].id, "fourth task should be task1")
        assertEquals(0, tasks[3].indent, "fourth task shouldn't be indented")
        assertEquals("00000000000000000001", tasks[3].position, "fourth task should be at position 1 (second task)")
    }

    @Test
    fun `indent task being a parent task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        repository.createAndGetTask(taskList.id, "task3")
        repository.indentTask(task1.id)
        // - list
        //    - task3 [0000]
        //    - task2 [0001]
        //      - task1 [0000]

        assertFailsWith<IllegalArgumentException>("Cannot indent task with subtasks") {
            repository.indentTask(task2.id)
        }
    }

    @Test
    fun `indent unavailable task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.indentTask(42L)
        }
    }

    @Test
    fun `indent top level at first position task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("list", "task1")

        assertFailsWith<IllegalArgumentException>("Cannot indent a top level task at first position") {
            repository.indentTask(task.id)
        }
    }

    @Test
    fun `indent subtask should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("list", "task1")
        val subtaskId = repository.createTask(taskList.id, task.id, "subtask")

        assertFailsWith<IllegalArgumentException>("Cannot indent a subtask") {
            repository.indentTask(subtaskId)
        }
    }

    @Test
    fun `sorting tasks by due date should honor due date and ignore parent task link and indentation`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val t1 = Clock.System.now() + 1.days
        repository.updateTaskDueDate(task1.id, t1)
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        val t2 = Clock.System.now() + 2.days
        repository.updateTaskDueDate(task2.id, t2)
        val task3 = repository.createAndGetTask(taskList.id, "task3")
        val t3 = Clock.System.now() + 3.days
        repository.updateTaskDueDate(task3.id, t3)
        val task4 = repository.createAndGetTask(taskList.id, "task4")
        val t4 = Clock.System.now() + 4.days
        repository.updateTaskDueDate(task4.id, t4)
        repository.indentTask(task3.id)
        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //   - task2 [0001]
        //   - task1 [0002]

        repository.sortTasksBy(taskList.id, TaskListSorting.DueDate)

        // list
        //   - task1 due in 1 days
        //   - task2 due in 2 days
        //   - task3 due in 3 days
        //   - task4 due in 4 days
        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(4, tasks.size, "should have 4 tasks in list")
        assertEquals(task1.id, tasks[0].id, "first task should be task1")
        assertEquals(0, tasks[0].indent, "task shouldn't be indented in due date sorting")
        assertEquals("00000000000000000002", tasks[0].position, "task position should remain unchanged in due date sorting")
        assertEquals(task2.id, tasks[1].id, "second task should be task2")
        assertEquals(0, tasks[1].indent, "task shouldn't be indented in due date sorting")
        assertEquals("00000000000000000001", tasks[1].position, "task position should remain unchanged in due date sorting")
        assertEquals(task3.id, tasks[2].id, "third task should be task3")
        assertEquals(0, tasks[2].indent, "task shouldn't be indented in due date sorting")
        assertEquals("00000000000000000000", tasks[2].position, "task position should remain unchanged in due date sorting")
        assertEquals(task4.id, tasks[3].id, "fourth task should be task4")
        assertEquals(0, tasks[3].indent, "task shouldn't be indented in due date sorting")
        assertEquals("00000000000000000000", tasks[3].position, "task position should remain unchanged in due date sorting")
    }

    @Test
    fun `sorting tasks manually should honor position & parent task link`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val t1 = Clock.System.now() + 1.days
        repository.updateTaskDueDate(task1.id, t1)
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        val t2 = Clock.System.now() + 2.days
        repository.updateTaskDueDate(task2.id, t2)
        val task3 = repository.createAndGetTask(taskList.id, "task3")
        val t3 = Clock.System.now() + 3.days
        repository.updateTaskDueDate(task1.id, t3)
        val task4 = repository.createAndGetTask(taskList.id, "task4")
        val t4 = Clock.System.now() + 4.days
        repository.updateTaskDueDate(task1.id, t4)
        repository.indentTask(task3.id)
        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //   - task2 [0001]
        //   - task1 [0002]
        repository.sortTasksBy(taskList.id, TaskListSorting.DueDate)
        // list
        //   - task1 due in 1 days
        //   - task2 due in 2 days
        //   - task3 due in 3 days
        //   - task4 due in 4 days

        repository.sortTasksBy(taskList.id, TaskListSorting.Manual)

        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //   - task2 [0001]
        //   - task1 [0002]
        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(4, tasks.size, "should have 4 tasks in list")
        assertEquals(task4.id, tasks[0].id, "first task should be task4")
        assertEquals(0, tasks[0].indent, "first task shouldn't be indented")
        assertEquals("00000000000000000000", tasks[0].position, "first task should be at position 0 (first task)")
        assertEquals(task3.id, tasks[1].id, "second task should be task3")
        assertEquals(1, tasks[1].indent, "second task should be a subtask indented by 1")
        assertEquals("00000000000000000000", tasks[1].position, "second task should be at position 0 (first subtask)")
        assertEquals(task2.id, tasks[2].id, "third task should be task2")
        assertEquals(0, tasks[2].indent, "third task shouldn't be indented")
        assertEquals("00000000000000000001", tasks[2].position, "third task should be at position 1 (second subtask)")
        assertEquals(task1.id, tasks[3].id, "fourth task should be task1")
        assertEquals(0, tasks[3].indent, "fourth task shouldn't be indented")
        assertEquals("00000000000000000002", tasks[3].position, "fourth task should be at position 1 (second task)")
    }

    @Test
    fun `unindent first subtask should put task right after original parent`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        val task3 = repository.createAndGetTask(taskList.id, "task3")
        repository.indentTask(task2.id)
        repository.indentTask(task1.id)
        // list
        //   - task3 [0000]
        //      - task2 [0000]
        //      - task1 [0001]

        repository.unindentTask(task2.id)

        // list
        //   - task3 [0000]
        //      - task1 [0000]
        //  - task2 [0001] * updated indentation & position
        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(3, tasks.size, "should have 3 tasks in list")
        assertEquals(task3.id, tasks[0].id, "first task should be task3")
        assertEquals(0, tasks[0].indent, "first task should be a task")
        assertEquals("00000000000000000000", tasks[0].position, "first task should be at position 0 (first task)")
        assertEquals(task1.id, tasks[1].id, "second task should be task1")
        assertEquals(1, tasks[1].indent, "second task should be a subtask indented by 1")
        assertEquals("00000000000000000000", tasks[1].position, "second task should be at position 0 (first subtask)")
        assertEquals(task2.id, tasks[2].id, "third task should be task2")
        assertEquals(0, tasks[2].indent, "third task shouldn't be indented")
        assertEquals("00000000000000000001", tasks[2].position, "third task should be at position 1 (second task)")
    }

    @Test
    fun `unindent first subtask should put task between original parent and its next sibling`() = runTaskRepositoryTest { repository ->
        val (taskList, task1) = repository.createAndGetTask("list", "task1")
        val task2 = repository.createAndGetTask(taskList.id, "task2")
        val task3 = repository.createAndGetTask(taskList.id, "task3")
        val task4 = repository.createAndGetTask(taskList.id, "task4")
        repository.indentTask(task3.id)
        repository.indentTask(task2.id)
        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //      - task2 [0001]
        //   - task1 [0001]

        repository.unindentTask(task2.id)

        // list
        //   - task4 [0000]
        //      - task3 [0000]
        //  - task2 [0001] * updated indentation & position
        //  - task1 [0002] * updated position
        val updatedTaskList = repository.findTaskListById(taskList.id)
        assertNotNull(updatedTaskList)
        val tasks = updatedTaskList.tasks
        assertEquals(4, tasks.size, "should have 4 tasks in list")
        assertEquals(task4.id, tasks[0].id, "first task should be task4")
        assertEquals(0, tasks[0].indent, "first task should be a task")
        assertEquals("00000000000000000000", tasks[0].position, "first task should be at position 0 (first task)")
        assertEquals(task3.id, tasks[1].id, "second task should be task3")
        assertEquals(1, tasks[1].indent, "second task should be a subtask indented by 1")
        assertEquals("00000000000000000000", tasks[1].position, "second task should be at position 0 (first subtask)")
        assertEquals(task2.id, tasks[2].id, "third task should be task2")
        assertEquals(0, tasks[2].indent, "third task shouldn't be indented")
        assertEquals("00000000000000000001", tasks[2].position, "third task should be at position 1 (second task)")
        assertEquals(task1.id, tasks[3].id, "fourth task should be task1")
        assertEquals(0, tasks[3].indent, "fourth task shouldn't be indented")
        assertEquals("00000000000000000002", tasks[3].position, "fourth task should be at position 2 (third task)")
    }

    @Test
    fun `unindent unavailable task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        assertFailsWith<IllegalArgumentException>("Invalid task id 42") {
            repository.unindentTask(42L)
        }
    }

    @Test
    fun `unindent top level task should throw IllegalArgumentException`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("list", "task1")

        assertFailsWith<IllegalArgumentException>("Cannot indent a top level task") {
            repository.unindentTask(task.id)
        }
    }
}