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

import kotlinx.coroutines.runBlocking
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import kotlin.system.exitProcess

fun main() {
    runBlocking {
        val tasksHttpClient = buildGoogleHttpClient(
            "https://tasks.googleapis.com",
            "client_secret_1018227543555-k121h4da66i87lpione39a7et0lkifqi.apps.googleusercontent.com.json",
            listOf(GoogleAuthenticator.Permission.Tasks)
        ) {
            println("Please open the following URL in your browser to authenticate:")
            println(it)
        }
        val taskListsApi = TaskListsApi(tasksHttpClient)
        val tasksApi = TasksApi(tasksHttpClient)
        val taskLists = taskListsApi.list()

        if (false) {
            val createdTaskList = taskListsApi.insert(TaskList(title = "Test task list"))
            if (createdTaskList.id.isEmpty()) {
                error("Failed to create task list")
            }
            if (createdTaskList.title != "Test task list") {
                error("Unexpected task list title: ${createdTaskList.title}")
            }

            val getIt = taskListsApi.get(createdTaskList.id)
            if (getIt.id != createdTaskList.id) {
                error("Failed to get task list")
            }

            println("Created task list: $createdTaskList")
        } else if (false) {
            val taskList = taskLists.items.find { it.title == "Test task list" } ?: error("Can't find task list 'Test task list'")
            tasksApi.list(taskList.id).items.filter { !it.isHidden && it.title == "My updated task" }.forEach {
//                tasksApi.update(taskList.id, it.id, it.copy(title = "My updated task"))
                tasksApi.patch(taskList.id, it.id, it.copy(title = "My patched task"))
            }
            val createdTask = tasksApi.insert(taskList.id, Task(title = "My new task", status = Task.Status.Completed), null, null)
            printTask(createdTask)
            tasksApi.delete(taskList.id, createdTask.id)
            tasksApi.list(taskList.id).items.filter { it.isHidden }.forEach {
                tasksApi.delete(taskList.id, it.id)
            }
            tasksApi.clear(taskList.id)
        }

        val testTaskList = taskListsApi.listAll().firstOrNull { it.title == "Test task list" }
        testTaskList?.let {
            taskListsApi.delete(it.id)
        }

        if (false) {
            taskListsApi.insert(TaskList(title = "This is a list")).let {
                tasksApi.insert(it.id, Task(title = "This is a task")).let { task ->
//                delay(2000)
                    tasksApi.patch(it.id, task.id, task.copy(title = "This is a task (patched)"))
//                delay(2000)
                    tasksApi.update(it.id, task.id, task.copy(title = "This is a task (updated)"))
//                delay(2000)
                    tasksApi.delete(it.id, task.id)
//                delay(2000)
                }
                val task = tasksApi.insert(it.id, Task(title = "This is a task2"))
//            delay(2000)
                tasksApi.patch(
                    it.id,
                    task.id,
                    task.copy(title = "This is a task2 COMP", status = Task.Status.Completed)
                )
//            delay(2000)
                tasksApi.clear(it.id)
//            delay(2000)

//            delay(2000)
                taskListsApi.patch(it.id, it.copy(title = "This is a list (patched)"))
//            delay(2000)
                taskListsApi.update(it.id, it.copy(title = "This is a list (updated)"))
//            delay(2000)
                taskListsApi.delete(it.id)
//            delay(2000)
            }
        }

        taskLists.items.forEach(::println)
        taskLists.items.find { it.title == "My Tasks" }?.let {
            tasksApi.list(it.id).items.forEach { task ->
                printTask(task)
            }
        }

        exitProcess(0)

        // TODO pagination
        var taskListId: String? = null
        var taskId: String? = null
        taskLists.items.forEach { taskList ->
            println("# Task list: $taskList")
            val tasks = tasksApi.list(taskList.id, showCompleted = true, showHidden = true)
            // TODO pagination
            tasks.items.forEach { task ->
                if (taskId == null) {
                    taskListId = taskList.id
                    taskId = task.id
                }
                printTask(task)
            }
        }

        taskId?.let {
            val task = tasksApi.get(taskListId!!, it)
            printTask(task)
        }
    }
}

fun printTask(task: Task) {
    val status = when (task.status) {
        Task.Status.NeedsAction -> "❌"
        Task.Status.Completed -> "✅"
    }
    println("  $status Task: $task")
}
