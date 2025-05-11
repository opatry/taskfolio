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
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.tasks.data.util.runTaskRepositoryTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private suspend fun TaskRepository.createAndGetTaskList(title: String): TaskListDataModel {
    createTaskList(title)
    return getTaskLists().firstOrNull()?.maxByOrNull(TaskListDataModel::id)?.takeIf { it.title == title }
        ?: error("Task list not found")
}

private suspend fun TaskRepository.createAndGetTask(taskListId: Long, taskTitle: String): TaskDataModel {
    createTask(taskListId, taskTitle)
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
    fun `delete task list`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.deleteTaskList(taskList.id)

        val taskLists = repository.getTaskLists().firstOrNull()
        assertNotNull(taskLists)
        assertEquals(0, taskLists.size, "No task list expected")
    }

    @Test
    fun `create task`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        val taskId = repository.createTask(taskList.id, "My task")

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

        repository.createTask(taskList.id, "task1")
        repository.createTask(taskList.id, "task2")

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
    fun `rename task`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskTitle(task.id, "My renamed task")

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("My renamed task", tasks.first().title)
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
    fun `complete task`() = runTaskRepositoryTest { repository ->
        val (taskList, task) = repository.createAndGetTask("My tasks", "My task")

        repository.toggleTaskCompletionState(task.id)

        val tasks = repository.findTaskListById(taskList.id)?.tasks
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertTrue(tasks.first().isCompleted)
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
}