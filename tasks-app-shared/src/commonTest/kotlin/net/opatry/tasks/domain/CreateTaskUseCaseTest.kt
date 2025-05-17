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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.Task
import net.opatry.tasks.NowProvider
import net.opatry.tasks.data.TaskDao
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class CreateTaskUseCaseTest {
    // FIXME shouldn't rely on Dao & API, only repository quick & dirty for now
    @Mock
    private lateinit var tasksApi: TasksApi

    @Mock
    private lateinit var taskListDao: TaskListDao

    @Mock
    private lateinit var taskDao: TaskDao

    @Mock
    private lateinit var nowProvider: NowProvider

    private lateinit var useCase: CreateTaskUseCase

    @BeforeTest
    fun setUp() {
        useCase = CreateTaskUseCase(
            taskListDao = taskListDao,
            taskDao = taskDao,
            tasksApi = tasksApi,
            clockNow = nowProvider
        )
    }

    @Test
    fun `when use case invoked then should create task and sync it`() = runTest {
        // Given
        val now = mock(Instant::class.java)
        val taskTitle = "My task"
        val taskListId = TaskListId(1)
        val taskList = mock(TaskListEntity::class.java).apply {
            `when`(remoteId).thenReturn("remoteId")
        }
        `when`(nowProvider.invoke()).thenReturn(now)
        `when`(taskListDao.getById(taskListId.value)).thenReturn(taskList)
        val firstPosition = "00000000000000000000"
        `when`(taskDao.getTasksFromPositionOnward(taskListId.value, null, firstPosition))
            .thenReturn(emptyList())
        `when`(
            taskDao.upsertAll(
                listOf(
                    TaskEntity(
                        parentListLocalId = taskListId.value,
                        parentTaskLocalId = null,
                        title = taskTitle,
                        notes = "",
                        lastUpdateDate = now,
                        dueDate = null,
                        position = firstPosition,
                    )
                )
            )
        ).thenReturn(listOf(100L))

        // When
        val taskId = useCase(taskListId, null, taskTitle)
        advanceUntilIdle()

        // Then
        assertEquals(TaskId(100L), taskId)
        then(tasksApi).should().insert(
            taskListId = "remoteId",
            task = Task(
                title = taskTitle,
                notes = "",
                updatedDate = now,
                position = firstPosition,
            )
        )
    }
}