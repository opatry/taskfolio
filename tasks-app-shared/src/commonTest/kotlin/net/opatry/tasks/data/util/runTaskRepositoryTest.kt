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

package net.opatry.tasks.data.util

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.tasks.InMemoryTasksApi
import net.opatry.tasks.NowProvider
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.TransactionRunner

internal suspend fun TaskRepository.printTaskTree() {
    getTaskLists().firstOrNull()?.let { taskLists ->
        if (taskLists.isEmpty()) {
            println("No task lists found.")
            return
        }
        for (taskList in taskLists) {
            println("- ${taskList.title} (#${taskList.tasks.count()})")
            taskList.tasks.forEach { task ->
                val tabs = "  ".repeat(task.indent + 1)
                println("$tabs- ${task.title} (@{${task.position}} >[${task.indent}])")
            }
        }
    } ?: println("Task lists not ready.")
}

private val TestTransactionRunner = object : TransactionRunner {
    override suspend fun <R> runInTransaction(logic: suspend () -> R): R = logic()
}

internal fun runTaskRepositoryTest(
    taskListsApi: TaskListsApi = InMemoryTaskListsApi(),
    tasksApi: TasksApi = InMemoryTasksApi(),
    test: suspend TestScope.(TaskRepository) -> Unit
) = runTest {
    val db = inMemoryTasksAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(backgroundScope.coroutineContext)
        .build()

    val repository = TaskRepository(
        transactionRunner = TestTransactionRunner,
        taskListDao = db.getTaskListDao(),
        taskDao = db.getTaskDao(),
        taskListsApi = taskListsApi,
        tasksApi = tasksApi,
        nowProvider = NowProvider(Clock.System::now)
    )
    try {
        test(repository)
    } catch (e: AssertionError) {
        repository.printTaskTree()
        throw e
    } finally {
        db.close()
    }
}
