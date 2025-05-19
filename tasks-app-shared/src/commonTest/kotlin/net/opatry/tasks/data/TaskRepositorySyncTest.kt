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
import net.opatry.tasks.InMemoryTasksApi
import net.opatry.tasks.data.model.TaskListDataModel
import net.opatry.tasks.data.util.InMemoryTaskListsApi
import net.opatry.tasks.data.util.runTaskRepositoryTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class TaskRepositorySyncTest {
    @Test
    fun `sync remote task lists`() {
        val taskListsApi = InMemoryTaskListsApi("My tasks", "Other tasks")
        val tasksApi = InMemoryTasksApi("1" to listOf("First task TODO"), "2" to listOf("Another task"))

        runTaskRepositoryTest(taskListsApi, tasksApi) { repository ->
            val initialTaskLists = repository.getTaskLists().firstOrNull()
            assertEquals(0, initialTaskLists?.size, "There shouldn't be any task list at start")

            repository.sync()

            assertEquals(2, taskListsApi.requestCount)
            assertContentEquals(listOf("default", "list"), taskListsApi.requests)
            assertEquals(2, tasksApi.requestCount)
            assertContentEquals(listOf("list", "list"), tasksApi.requests)

            val taskLists = repository.getTaskLists().firstOrNull()
            assertNotNull(taskLists)
            assertEquals(2, taskLists.size)
            assertContentEquals(listOf("My tasks", "Other tasks"), taskLists.map(TaskListDataModel::title))

            val firstTaskListTasks = taskLists[0].tasks
            assertEquals(1, firstTaskListTasks.size)
            assertEquals("First task TODO", firstTaskListTasks.firstOrNull()?.title)

            val secondTaskListTasks = taskLists[1].tasks
            assertEquals(1, secondTaskListTasks.size)
            assertEquals("Another task", secondTaskListTasks.firstOrNull()?.title)
        }
    }

    @Test
    fun `task list CRUD without network should create a local only task list`() {
        val taskListsApi = InMemoryTaskListsApi()

        runTaskRepositoryTest(taskListsApi) { repository ->
            taskListsApi.isNetworkAvailable = false
            repository.createTaskList("Task list")
            // TODO check remote id is null somehow
            assertEquals(1, repository.getTaskLists().firstOrNull()?.size)
        }

        assertEquals(0, taskListsApi.requestCount)
    }

    @Test
    fun `local only task lists are synced at next sync`() {
        val taskListsApi = InMemoryTaskListsApi()

        runTaskRepositoryTest(taskListsApi) { repository ->
            // for first request, no network
            taskListsApi.isNetworkAvailable = false
            repository.createTaskList("Task list")
            val taskList = repository.getTaskLists().firstOrNull()?.firstOrNull()
            assertNotNull(taskList)
            assertEquals(0, taskListsApi.requestCount)

            // network is considered back, sync should trigger fetch & push requests
            taskListsApi.isNetworkAvailable = true
            repository.sync()
            assertEquals(3, taskListsApi.requestCount)
            assertContentEquals(listOf("default", "list", "insert"), taskListsApi.requests)
        }
    }
}