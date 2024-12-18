/*
 * Copyright (c) 2024 Olivier Patry
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
import kotlin.test.assertTrue

private suspend fun TaskRepository.createAndGetTaskList(title: String): TaskListDataModel {
    createTaskList(title)
    return getTaskLists().firstOrNull()?.firstOrNull() ?: error("Task list not found")
}

private suspend fun TaskRepository.createAndGetTask(taskListId: Long, taskTitle: String): TaskDataModel {
    createTask(taskListId, taskTitle)
    return getTaskLists().firstOrNull()
        ?.firstOrNull { it.id == taskListId }
        ?.tasks
        ?.firstOrNull { it.title == taskTitle }
        ?: error("Task not found")
}

private suspend fun TaskRepository.createAndGetTask(taskListTitle: String, taskTitle: String): Pair<TaskListDataModel, TaskDataModel> {
    val taskList = createAndGetTaskList(taskListTitle)
    return taskList to createAndGetTask(taskList.id, taskTitle)
}

class TaskRepositoryCRUDTest {

    @Test
    fun `create task list`() = runTaskRepositoryTest { repository ->
        repository.createTaskList("My tasks")

        val taskLists = repository.getTaskLists().firstOrNull()
        assertEquals(1, taskLists?.size)
        assertEquals("My tasks", taskLists?.first()?.title)
    }

    @Test
    fun `rename task list`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.renameTaskList(taskList.id, "My renamed list")
        val taskListRenamed = repository.getTaskLists().firstOrNull()?.firstOrNull()
        assertEquals("My renamed list", taskListRenamed?.title, "Updated name is invalid")
    }

    @Test
    fun `delete task list`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.deleteTaskList(taskList.id)
        val taskLists = repository.getTaskLists().firstOrNull()
        assertEquals(0, taskLists?.size, "No task list expected")
    }

    @Test
    fun `create task`() = runTaskRepositoryTest { repository ->
        val taskList = repository.createAndGetTaskList("My tasks")

        repository.createTask(taskList.id, "My task")
        val tasks = repository.getTaskLists().firstOrNull()?.firstOrNull()?.tasks
        assertEquals(1, tasks?.size)
        assertEquals("My task", tasks?.first()?.title)
        assertFalse(tasks?.first()?.isCompleted ?: true)
    }

    @Test
    fun `rename task`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskTitle(task.id, "My renamed task")
        val tasks = repository.getTaskLists().firstOrNull()?.firstOrNull()?.tasks
        assertEquals("My renamed task", tasks?.first()?.title)
    }

    @Test
    fun `edit task notes`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("My tasks", "My task")

        repository.updateTaskNotes(task.id, "These are some notes")
        val tasks = repository.getTaskLists().firstOrNull()?.firstOrNull()?.tasks
        assertEquals("These are some notes", tasks?.first()?.notes)
    }

    @Test
    fun `complete task`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("My tasks", "My task")

        repository.toggleTaskCompletionState(task.id)
        val tasks = repository.getTaskLists().firstOrNull()?.firstOrNull()?.tasks
        assertTrue(tasks?.first()?.isCompleted ?: false)
    }

    @Test
    fun `delete task`() = runTaskRepositoryTest { repository ->
        val (_, task) = repository.createAndGetTask("My tasks", "My task")

        repository.deleteTask(task.id)
        val tasks = repository.getTaskLists().firstOrNull()?.firstOrNull()?.tasks
        assertEquals(0, tasks?.size, "Task should have been deleted")
    }
}