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

package net.opatry.tasks.domain

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.util.inMemoryTasksAppDatabaseBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private fun runWithInMemoryDatabase(
    test: suspend TestScope.(TaskListDao) -> Unit
) = runTest {
    val db = inMemoryTasksAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(backgroundScope.coroutineContext)
        .build()

    try {
        test(db.getTaskListDao())
    } finally {
        db.close()
    }
}

@RunWith(MockitoJUnitRunner::class)
class CreateTaskListUseCaseTest {
    // FIXME shouldn't rely on Dao & API, only repository quick & dirty for now
    @Mock
    private lateinit var taskListsApi: TaskListsApi

    @Test
    fun `when use case invoked then should create task list and sync it`() = runWithInMemoryDatabase { taskListDao ->
        val now = Instant.fromEpochMilliseconds(42L)
        val useCase = CreateTaskListUseCase(taskListDao, taskListsApi) { now }
        val taskListId = useCase("My tasks")

        val taskLists = taskListDao.getAllAsFlow().firstOrNull()
        assertNotNull(taskLists)
        assertEquals(1, taskLists.size)
        assertEquals("My tasks", taskLists.first().title)
        assertEquals(taskListId.value, taskLists.first().id)

        then(taskListsApi).should().insert(
            TaskList(
                title = "My tasks",
                updatedDate = now,
            )
        )
    }
}