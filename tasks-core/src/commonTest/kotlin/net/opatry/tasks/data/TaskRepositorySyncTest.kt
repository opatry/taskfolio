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


import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.model.ResourceListResponse
import net.opatry.tasks.NowProvider
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import net.opatry.google.tasks.model.Task as RemoteTask
import net.opatry.google.tasks.model.TaskList as RemoteTaskList
import net.opatry.tasks.data.entity.TaskEntity as LocalTask
import net.opatry.tasks.data.entity.TaskListEntity as LocalTaskList

@RunWith(MockitoJUnitRunner::class)
class TaskRepositorySyncTest {
    //region Mocks

    @Mock
    private lateinit var taskListDao: TaskListDao

    @Mock
    private lateinit var taskDao: TaskDao

    @Mock
    private lateinit var taskListsApi: TaskListsApi

    @Mock
    private lateinit var tasksApi: TasksApi

    @Mock
    private lateinit var nowProvider: NowProvider

    @InjectMocks
    private lateinit var repository: TaskRepository

    private fun mockRemoteTaskListResponse(
        remoteId: String,
        title: String,
        updatedDate: Instant,
    ): ResourceListResponse<RemoteTaskList> =
        mock {
            on { items } doReturn listOf(RemoteTaskList(id = remoteId, title = title, updatedDate = updatedDate))
            on { nextPageToken } doReturn null
        }

    private fun mockNoRemoteTaskListsResponse(): ResourceListResponse<RemoteTaskList> =
        mock {
            on { items } doReturn emptyList()
            on { nextPageToken } doReturn null
        }

    private suspend fun stubRemoteTaskListsFailure() {
        whenever(taskListsApi.list(maxResults = 100, null))
            .thenThrow(ServerResponseException::class.java)
    }

    private suspend fun stubNoRemoteTaskLists() {
        val listsResponse = mockNoRemoteTaskListsResponse()
        whenever(taskListsApi.list(maxResults = 100, null))
            .thenReturn(listsResponse)
    }

    private suspend fun stubRemoteTaskList(
        remoteId: String = "remoteId",
        title: String = "list",
        updatedDate: Instant = Clock.System.now(),
    ) {
        val listsResponse = mockRemoteTaskListResponse(remoteId = remoteId, title = title, updatedDate = updatedDate)
        whenever(taskListsApi.list(maxResults = 100, null))
            .thenReturn(listsResponse)
    }

    private suspend fun stubRemoteTaskListSynced(
        remoteId: String = "remoteId",
        title: String = "list",
        updatedDate: Instant = Clock.System.now(),
    ): Long {
        stubRemoteTaskList(remoteId = remoteId, title = title, updatedDate = updatedDate)
        val localTaskList = LocalTaskList(id = 42, remoteId = remoteId, title = title, lastUpdateDate = updatedDate)
        whenever(taskListDao.getByRemoteId(remoteId)).thenReturn(localTaskList)
        whenever(taskListDao.upsertAll(listOf(localTaskList)))
            .thenReturn(listOf(localTaskList.id))
        return localTaskList.id
    }

    private fun mockRemoteTasksResponse(vararg remoteTasks: RemoteTask): ResourceListResponse<RemoteTask> =
        mock {
            on { items } doReturn remoteTasks.toList()
            on { nextPageToken } doReturn null
        }

    private suspend fun stubRemoteTasks(
        taskListId: String,
        updatedMin: Instant?,
        vararg remoteTasks: RemoteTask,
    ) {
        val response = mockRemoteTasksResponse(*remoteTasks)
        whenever(
            tasksApi.list(
                taskListId = taskListId,
                showDeleted = false,
                showHidden = true,
                showCompleted = true,
                maxResults = 100,
                updatedMin = updatedMin,
                completedMin = null,
                completedMax = null,
                dueMin = null,
                dueMax = null,
            )
        ).thenReturn(response)
    }

    private suspend fun stubNoRemoteTasks(
        taskListId: String,
        updatedMin: Instant? = null,
    ) {
        val response = mockRemoteTasksResponse(*emptyArray())
        whenever(
            tasksApi.list(
                taskListId = taskListId,
                showDeleted = false,
                showHidden = true,
                showCompleted = true,
                maxResults = 100,
                updatedMin = updatedMin,
                completedMin = null,
                completedMax = null,
                dueMin = null,
                dueMax = null,
            )
        ).thenReturn(response)
    }

    private suspend fun stubNoLocalOnlyTaskLists() {
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(emptyList())
    }

    private suspend fun stubNoLocalOnlyTasks(taskListId: Long) {
        whenever(taskDao.getLocalOnlyTasks(taskListId)).thenReturn(emptyList())
    }

    //endregion

    //region TaskList pull

    @Test
    fun `when remote listing fails then sync should do nothing`() = runTest {
        // Given
        stubRemoteTaskListsFailure()

        // When
        repository.sync(false)

        // Then
        verify(taskListsApi).list(maxResults = 100, null)
        verifyNoMoreInteractions(taskListsApi)
        verifyNoInteractions(taskListDao, taskDao, tasksApi, nowProvider)
    }

    @Test
    fun `when no remote lists then sync should do almost nothing`() = runTest {
        // Given
        stubNoRemoteTaskLists()
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(42)
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(taskListsApi).list(maxResults = 100, null)
        verify(taskListDao).getLocalOnlyTaskLists()
        verify(nowProvider).now()
        verifyNoMoreInteractions(taskListsApi, taskListDao, nowProvider)
        verifyNoInteractions(taskDao, tasksApi)
    }

    @Test
    fun `when delete stale tasks is requested then sync should call clean stale tasks`() = runTest {
        // Given
        stubNoRemoteTaskLists()
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(42)
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        val spyRepository = spy(repository)
        spyRepository.sync(true)

        // Then
        verify(taskListsApi).list(maxResults = 100, null)
        verify(taskListDao).getLocalOnlyTaskLists()
        verify(taskListDao).deleteStaleTaskLists(emptyList())
        verify(nowProvider).now()
        verify(spyRepository).cleanStaleTasks(emptyList())
        verifyNoMoreInteractions(taskListsApi, taskListDao, nowProvider)
        verifyNoInteractions(taskDao, tasksApi)
    }

    @Test
    fun `when remote list then sync should create a local list`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        stubRemoteTaskList(remoteId = "remoteId", title = "list", updatedDate = updatedDate)
        whenever(taskListDao.getByRemoteId("remoteId")).thenReturn(null)
        val localTaskList = LocalTaskList(
            id = 0,
            remoteId = "remoteId",
            title = "list",
            lastUpdateDate = updatedDate
        )
        whenever(taskListDao.upsertAll(listOf(localTaskList)))
            .thenReturn(listOf(42))
        stubNoRemoteTasks(taskListId = "remoteId")
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(42)

        // When
        repository.sync(false)

        // Then
        verify(taskListDao).upsertAll(listOf(localTaskList))
    }

    @Test
    fun `when remote list is renamed then sync should rename local counterpart`() = runTest {
        // Given
        val oldDate = Clock.System.now().minus(3.days)
        val updatedDate = Clock.System.now()
        stubRemoteTaskList(remoteId = "remoteId", title = "renamed list", updatedDate = updatedDate)
        val localTaskList = LocalTaskList(
            id = 42,
            remoteId = "remoteId",
            title = "list",
            lastUpdateDate = oldDate,
            sorting = LocalTaskList.Sorting.Title,
        )
        whenever(taskListDao.getByRemoteId("remoteId")).thenReturn(localTaskList)
        val localTaskListUpdated = localTaskList.copy(title = "renamed list", lastUpdateDate = updatedDate)
        whenever(taskListDao.upsertAll(listOf(localTaskListUpdated)))
            .thenReturn(listOf(42))
        stubNoRemoteTasks(taskListId = "remoteId")
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(42)

        // When
        repository.sync(false)

        // Then
        verify(taskListDao).upsertAll(listOf(localTaskListUpdated))
    }

    //endregion

    //region Task pull

    @Test
    fun `when remote task then sync should create a local task`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val localTaskListId = stubRemoteTaskListSynced(remoteId = "remoteListId")
        val remoteTask = RemoteTask(id = "remoteTaskId", title = "task", updatedDate = updatedDate)
        stubRemoteTasks(taskListId = "remoteListId", updatedMin = null, remoteTask)
        whenever(taskDao.getByRemoteId("remoteTaskId")).thenReturn(null)
        val localTask = remoteTask.asTaskEntity(parentListLocalId = localTaskListId, parentTaskLocalId = null, taskLocalId = null)
        whenever(taskDao.upsertAll(listOf(localTask)))
            .thenReturn(listOf(100))
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(localTaskListId)

        // When
        repository.sync(false)

        // Then
        verify(taskDao).upsertAll(listOf(localTask))
    }

    @Test
    fun `when remote subtask then sync should create a local subtask`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val localTaskListId = stubRemoteTaskListSynced(remoteId = "remoteListId")
        val remoteParentTask = RemoteTask(id = "remoteParentTaskId", title = "parent task", updatedDate = updatedDate)
        val remoteChildTask = RemoteTask(id = "remoteChildTaskId", title = "child task", updatedDate = updatedDate, parent = "remoteParentTaskId")
        stubRemoteTasks(taskListId = "remoteListId", updatedMin = null, remoteParentTask, remoteChildTask)
        whenever(taskDao.getByRemoteId("remoteParentTaskId")).thenReturn(null)
        val localParentTask = remoteParentTask.asTaskEntity(parentListLocalId = localTaskListId, parentTaskLocalId = null, taskLocalId = null)
        whenever(taskDao.getByRemoteId("remoteChildTaskId")).thenReturn(null)
        val localChildTask = remoteChildTask.asTaskEntity(parentListLocalId = localTaskListId, parentTaskLocalId = null, taskLocalId = null)
        whenever(taskDao.upsertAll(listOf(localParentTask, localChildTask)))
            .thenReturn(listOf(100, 101))
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(localTaskListId)

        // When
        repository.sync(false)

        // Then
        verify(taskDao).upsertAll(listOf(localParentTask, localChildTask))
    }

    @Test
    fun `when sync 2 times then last sync time should be used to query remote tasks`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val syncTime1 = Clock.System.now()
        val syncTime2 = Clock.System.now()
        val localTaskListId = stubRemoteTaskListSynced(remoteId = "remoteListId")
        val remoteParentTask = RemoteTask(id = "remoteParentTaskId", title = "parent task", updatedDate = updatedDate)
        val remoteChildTask = RemoteTask(id = "remoteChildTaskId", title = "child task", updatedDate = updatedDate, parent = "remoteParentTaskId")
        whenever(nowProvider.now()).thenReturn(syncTime1, syncTime2)
        // first listing should provide no time
        stubRemoteTasks(taskListId = "remoteListId", updatedMin = null, remoteParentTask, remoteChildTask)
        whenever(taskDao.getByRemoteId("remoteParentTaskId")).thenReturn(null)
        val localParentTask = remoteParentTask.asTaskEntity(parentListLocalId = localTaskListId, parentTaskLocalId = null, taskLocalId = null)
        whenever(taskDao.getByRemoteId("remoteChildTaskId")).thenReturn(null)
        val localChildTask = remoteChildTask.asTaskEntity(parentListLocalId = localTaskListId, parentTaskLocalId = null, taskLocalId = null)
        whenever(taskDao.upsertAll(listOf(localParentTask, localChildTask)))
            .thenReturn(listOf(100, 101))
        // second listing should provide previous sync time
        stubNoRemoteTasks(taskListId = "remoteListId", updatedMin = syncTime1)
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(localTaskListId)

        // When
        repository.sync(false)
        repository.sync(false)

        // Then
        verify(taskDao).upsertAll(listOf(localParentTask, localChildTask))
        verify(tasksApi).list(
            taskListId = "remoteListId",
            showDeleted = false,
            showHidden = true,
            showCompleted = true,
            maxResults = 100,
            updatedMin = null,
            completedMin = null,
            completedMax = null,
            dueMin = null,
            dueMax = null,
        )
        verify(tasksApi).list(
            taskListId = "remoteListId",
            showDeleted = false,
            showHidden = true,
            showCompleted = true,
            maxResults = 100,
            updatedMin = syncTime1,
            completedMin = null,
            completedMax = null,
            dueMin = null,
            dueMax = null,
        )
        then(nowProvider).should(times(2)).now()
    }

    @Test
    fun `when remote task is updated then sync should rename local counterpart`() = runTest {
        // Given
        val oldDate = Clock.System.now().minus(3.days)
        val updatedDate = Clock.System.now()
        val localTaskListId = stubRemoteTaskListSynced(remoteId = "remoteListId")
        val remoteTask = RemoteTask(id = "remoteTaskId", title = "renamed task", notes = "added notes", updatedDate = updatedDate)
        stubRemoteTasks(taskListId = "remoteListId", updatedMin = null, remoteTask)
        val localTask = LocalTask(
            id = 100,
            remoteId = remoteTask.id,
            parentListLocalId = localTaskListId,
            title = "task",
            notes = "",
            lastUpdateDate = oldDate,
            position = ""
        )
        whenever(taskDao.getByRemoteId("remoteTaskId")).thenReturn(localTask)
        val localTaskUpdated = localTask.copy(title = remoteTask.title, notes = remoteTask.notes ?: "", lastUpdateDate = remoteTask.updatedDate)
        whenever(taskDao.upsertAll(listOf(localTaskUpdated)))
            .thenReturn(listOf(localTask.id))
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(localTaskListId)

        // When
        repository.sync(false)

        // Then
        verify(taskDao).upsertAll(listOf(localTaskUpdated))
    }

    @Ignore("TODO")
    @Test
    fun `when remote task is moved from one list to another then sync should reflect change to local counterparts`() = runTest {
    }

    @Test
    fun `when remote task is indented then sync should reflect change to local counterpart`() = runTest {
        // Given
        val oldDate = Clock.System.now().minus(3.days)
        val updatedDate = Clock.System.now()
        val localTaskListId = stubRemoteTaskListSynced(remoteId = "remoteListId")
        val remoteParentTask = RemoteTask(id = "remoteParentTaskId", title = "task1", updatedDate = oldDate)
        val remoteChildTask = RemoteTask(id = "remoteChildTaskId", title = "task2", updatedDate = updatedDate, parent = "remoteParentTaskId")
        stubRemoteTasks(taskListId = "remoteListId", updatedMin = null, remoteParentTask, remoteChildTask)
        val localTask1 = LocalTask(
            id = 100,
            remoteId = remoteParentTask.id,
            parentListLocalId = localTaskListId,
            parentTaskRemoteId = null,
            parentTaskLocalId = null,
            title = "task1",
            notes = "",
            lastUpdateDate = oldDate,
            position = ""
        )
        whenever(taskDao.getByRemoteId("remoteParentTaskId")).thenReturn(localTask1)
        val localTask2 = LocalTask(
            id = 101,
            remoteId = remoteChildTask.id,
            parentListLocalId = localTaskListId,
            parentTaskRemoteId = null,
            parentTaskLocalId = null,
            title = "task2",
            notes = "",
            lastUpdateDate = oldDate,
            position = ""
        )
        whenever(taskDao.getByRemoteId("remoteChildTaskId")).thenReturn(localTask2)
        val localTask2Updated = localTask2.copy(
            parentTaskRemoteId = "remoteParentTaskId",
            parentTaskLocalId = localTask1.id,
            lastUpdateDate = updatedDate,
        )
        whenever(taskDao.upsertAll(listOf(localTask1, localTask2Updated)))
            .thenReturn(listOf(localTask1.id, localTask2.id))
        stubNoLocalOnlyTaskLists()
        stubNoLocalOnlyTasks(localTaskListId)

        // When
        repository.sync(false)

        // Then
        verify(taskDao).upsertAll(listOf(localTask1, localTask2Updated))
    }

    @Ignore("TODO")
    @Test
    fun `when remote task is moved then sync should reflect change to local counterpart`() = runTest {
    }

    //endregion

    //region TaskList push

    @Test
    fun `when local list then sync should create a remote list`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        stubNoRemoteTaskLists()
        val localList = LocalTaskList(id = 42, remoteId = null, title = "list", lastUpdateDate = updatedDate)
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(listOf(localList))
        val localListAsRemote = RemoteTaskList(title = localList.title, updatedDate = updatedDate)
        val resultingRemoteList = localListAsRemote.copy(id = "remoteId", updatedDate = updatedDate.plus(1.seconds))
        whenever(taskListsApi.insert(localListAsRemote))
            .thenReturn(resultingRemoteList)
        val localListUpdated = localList.copy(remoteId = resultingRemoteList.id, lastUpdateDate = resultingRemoteList.updatedDate)
        whenever(taskListDao.upsertAll(listOf(localListUpdated)))
            .thenReturn(listOf(localList.id))
        stubNoLocalOnlyTasks(localList.id)
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(taskListsApi).insert(localListAsRemote)
        verify(taskListDao).upsertAll(listOf(localListUpdated))
    }

    @Test
    fun `when local list with task then sync should create a remote list and remote task`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        stubNoRemoteTaskLists()
        val localList = LocalTaskList(id = 42, remoteId = null, title = "list", lastUpdateDate = updatedDate)
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(listOf(localList))
        val localListAsRemote = RemoteTaskList(title = localList.title, updatedDate = updatedDate)
        val resultingRemoteList = localListAsRemote.copy(id = "remoteListId", updatedDate = updatedDate.plus(1.seconds))
        whenever(taskListsApi.insert(localListAsRemote))
            .thenReturn(resultingRemoteList)
        val localListUpdated = localList.copy(remoteId = resultingRemoteList.id, lastUpdateDate = resultingRemoteList.updatedDate)
        whenever(taskListDao.upsertAll(listOf(localListUpdated)))
            .thenReturn(listOf(localList.id))
        val localTask = LocalTask(
            id = 100,
            remoteId = null,
            title = "task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localList.id,
            parentTaskLocalId = null,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        whenever(taskDao.getLocalOnlyTasks(localList.id)).thenReturn(listOf(localTask))
        val localTaskAsRemote = localTask.asTask()
        val resultingRemoteTask = localTaskAsRemote.copy(
            id = "remoteTaskId",
            updatedDate = updatedDate.plus(1.seconds),
        )
        whenever(tasksApi.insert("remoteListId", localTaskAsRemote))
            .thenReturn(resultingRemoteTask)
        val localTaskUpdated = localTask.copy(remoteId = resultingRemoteTask.id, lastUpdateDate = resultingRemoteTask.updatedDate)
        whenever(taskDao.upsertAll(listOf(localTaskUpdated)))
            .thenReturn(listOf(localTask.id))
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(taskListsApi).insert(localListAsRemote)
        verify(taskListDao).upsertAll(listOf(localListUpdated))
        verify(tasksApi).insert("remoteListId", localTaskAsRemote)
        verify(taskDao).upsertAll(listOf(localTaskUpdated))
    }

    @Test
    fun `when synced list with local task then sync should create a remote task in remote list`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val localListId = stubRemoteTaskListSynced(remoteId = "remoteListId", title = "list", updatedDate = updatedDate)
        stubNoRemoteTasks(taskListId = "remoteListId", updatedMin = null)
        stubNoLocalOnlyTaskLists()
        val localTask = LocalTask(
            id = 100,
            remoteId = null,
            title = "task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localListId,
            parentTaskLocalId = null,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        whenever(taskDao.getLocalOnlyTasks(localListId)).thenReturn(listOf(localTask))
        val localTaskAsRemote = localTask.asTask()
        val resultingRemoteTask = localTaskAsRemote.copy(
            id = "remoteTaskId",
            updatedDate = updatedDate.plus(1.seconds),
        )
        whenever(tasksApi.insert("remoteListId", localTaskAsRemote))
            .thenReturn(resultingRemoteTask)
        val localTaskUpdated = localTask.copy(remoteId = resultingRemoteTask.id, lastUpdateDate = resultingRemoteTask.updatedDate)
        whenever(taskDao.upsertAll(listOf(localTaskUpdated)))
            .thenReturn(listOf(localTask.id))
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(tasksApi).insert("remoteListId", localTaskAsRemote)
        verify(taskDao).upsertAll(listOf(localTaskUpdated))
    }

    @Test
    fun `when local list with task and subtask then sync should create a remote list, remote task and remote subtask`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        stubNoRemoteTaskLists()
        val localList = LocalTaskList(id = 42, remoteId = null, title = "list", lastUpdateDate = updatedDate)
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(listOf(localList))
        val localListAsRemote = RemoteTaskList(title = localList.title, updatedDate = updatedDate)
        val resultingRemoteList = localListAsRemote.copy(id = "remoteListId", updatedDate = updatedDate.plus(1.seconds))
        whenever(taskListsApi.insert(localListAsRemote))
            .thenReturn(resultingRemoteList)
        val localListUpdated = localList.copy(remoteId = resultingRemoteList.id, lastUpdateDate = resultingRemoteList.updatedDate)
        whenever(taskListDao.upsertAll(listOf(localListUpdated)))
            .thenReturn(listOf(localList.id))
        val localParentTask = LocalTask(
            id = 100,
            remoteId = null,
            title = "parent task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localList.id,
            parentTaskLocalId = null,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        val localChildTask = LocalTask(
            id = 101,
            remoteId = null,
            title = "child task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localList.id,
            parentTaskLocalId = localParentTask.id,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        whenever(taskDao.getLocalOnlyTasks(localList.id))
            .thenReturn(listOf(localParentTask, localChildTask))
        val localParentTaskAsRemote = localParentTask.asTask()
        val resultingRemoteParentTask = localParentTaskAsRemote.copy(
            id = "remoteParentTaskId",
            updatedDate = updatedDate.plus(1.seconds),
            parent = null,
        )
        val localChildTaskAsRemote = localChildTask.asTask()
        val resultingRemoteChildTask = localChildTaskAsRemote.copy(
            id = "remoteChildTaskId",
            updatedDate = updatedDate.plus(2.seconds),
            parent = resultingRemoteParentTask.id,
        )
        val localParentTaskUpdated =
            localParentTask.copy(
                remoteId = resultingRemoteParentTask.id,
                lastUpdateDate = resultingRemoteParentTask.updatedDate,
                parentTaskLocalId = null,
                parentTaskRemoteId = null,
            )
        whenever(tasksApi.insert("remoteListId", localParentTaskAsRemote, null))
            .thenReturn(resultingRemoteParentTask)
        val localChildTaskUpdated = localChildTask.copy(
            remoteId = resultingRemoteChildTask.id,
            lastUpdateDate = resultingRemoteChildTask.updatedDate,
            parentTaskLocalId = localParentTask.id,
            parentTaskRemoteId = resultingRemoteParentTask.id,
        )
        whenever(tasksApi.insert("remoteListId", localChildTaskAsRemote, resultingRemoteParentTask.id))
            .thenReturn(resultingRemoteChildTask)
        whenever(taskDao.upsertAll(listOf(localParentTaskUpdated, localChildTaskUpdated)))
            .thenReturn(listOf(localParentTask.id, localChildTask.id))
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(taskListsApi).insert(localListAsRemote)
        verify(taskListDao).upsertAll(listOf(localListUpdated))
        verify(tasksApi).insert(taskListId = "remoteListId", task = localParentTaskAsRemote, parentTaskId = null)
        verify(tasksApi).insert(taskListId = "remoteListId", task = localChildTaskAsRemote, parentTaskId = resultingRemoteParentTask.id)
        verify(taskDao).upsertAll(listOf(localParentTaskUpdated, localChildTaskUpdated))
    }

    //endregion

    //region Task push

    @Test
    fun `when local task then sync should create a remote task`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val remoteTaskListId = "remoteListId"
        val localTaskListId = stubRemoteTaskListSynced(remoteTaskListId)
        stubNoRemoteTasks(taskListId = remoteTaskListId)
        stubNoLocalOnlyTaskLists()
        val localTask = LocalTask(
            id = 100,
            remoteId = null,
            parentListLocalId = localTaskListId,
            title = "task",
            notes = "",
            lastUpdateDate = updatedDate,
            position = "00000000000000000000"
        )
        whenever(taskDao.getLocalOnlyTasks(localTaskListId)).thenReturn(listOf(localTask))
        val remoteTask = localTask.asTask()
        val remoteTaskUpdated = remoteTask.copy(id = "remoteTaskId", updatedDate = updatedDate.plus(1.seconds))
        whenever(tasksApi.insert(remoteTaskListId, remoteTask)).thenReturn(remoteTaskUpdated)
        val localTaskUpdated = remoteTaskUpdated.asTaskEntity(localTaskListId, null, localTask.id)

        // When
        repository.sync(false)

        // Then
        verify(tasksApi).insert(remoteTaskListId, remoteTask)
        verify(taskDao).upsertAll(listOf(localTaskUpdated))
    }

    @Test
    fun `when local subtask then sync should create a remote subtask`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val remoteTaskListId = "remoteListId"
        val localTaskListId = stubRemoteTaskListSynced(remoteTaskListId)
        val remoteParentTask = RemoteTask(id = "remoteParentTaskId", title = "parent task", updatedDate = updatedDate)
        stubRemoteTasks(taskListId = remoteTaskListId, updatedMin = null, remoteParentTask)
        stubNoLocalOnlyTaskLists()
        val localParentTask = LocalTask(
            id = 100,
            remoteId = remoteParentTask.id,
            title = "parent task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localTaskListId,
            parentTaskLocalId = null,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        val localChildTask = LocalTask(
            id = 101,
            remoteId = null,
            title = "child task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localTaskListId,
            parentTaskLocalId = localParentTask.id,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        whenever(taskDao.getLocalOnlyTasks(localTaskListId)).thenReturn(listOf(localChildTask))
        val remoteChildTask = localChildTask.asTask()
        val remoteChildTaskUpdated = remoteChildTask.copy(
            id = "remoteChildTaskId",
            updatedDate = updatedDate.plus(1.seconds),
            parent = remoteParentTask.id,
        )
        whenever(tasksApi.insert(remoteTaskListId, remoteChildTask)).thenReturn(remoteChildTaskUpdated)
        val localTaskUpdated = remoteChildTaskUpdated.asTaskEntity(localTaskListId, localParentTask.id, localChildTask.id)

        // When
        repository.sync(false)

        // Then
        verify(tasksApi).insert(remoteTaskListId, remoteChildTask)
        verify(taskDao).upsertAll(listOf(localTaskUpdated))
    }

    @Test
    fun `when local list sync fails then task sync should be ignored`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        stubNoRemoteTaskLists()
        val localList = LocalTaskList(id = 42, remoteId = null, title = "list", lastUpdateDate = updatedDate)
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(listOf(localList))
        val remoteList = RemoteTaskList(title = localList.title, updatedDate = localList.lastUpdateDate)
        whenever(taskListsApi.insert(remoteList)).thenThrow(ServerResponseException::class.java)

        // When
        repository.sync(false)

        // Then
        verify(taskListDao).getLocalOnlyTaskLists()
        verifyNoMoreInteractions(taskListDao)
        verifyNoInteractions(tasksApi, taskDao)
    }

    @Test
    fun `when local parent task sync fails then subtask sync should be ignored`() = runTest {
        // Given
        val updatedDate = Clock.System.now()
        val remoteListId = "remoteListId"
        val localListId = stubRemoteTaskListSynced(remoteListId)
        whenever(taskListDao.getLocalOnlyTaskLists()).thenReturn(emptyList())
        stubNoRemoteTasks(remoteListId)
        val localParentTask = LocalTask(
            id = 100,
            remoteId = null,
            title = "parent task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localListId,
            parentTaskLocalId = null,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        val localChildTask = LocalTask(
            id = 101,
            remoteId = null,
            title = "child task",
            lastUpdateDate = updatedDate,
            parentListLocalId = localListId,
            parentTaskLocalId = localParentTask.id,
            parentTaskRemoteId = null,
            position = "00000000000000000000",
        )
        whenever(taskDao.getLocalOnlyTasks(localListId))
            .thenReturn(listOf(localParentTask, localChildTask))
        val localParentTaskAsRemote = localParentTask.asTask()
        whenever(tasksApi.insert("remoteListId", localParentTaskAsRemote, null))
            .thenThrow(ServerResponseException::class.java)
        whenever(nowProvider.now()).thenReturn(mock())

        // When
        repository.sync(false)

        // Then
        verify(tasksApi).list(
            taskListId = "remoteListId",
            showDeleted = false,
            showHidden = true,
            showCompleted = true,
            maxResults = 100,
            updatedMin = null,
            completedMin = null,
            completedMax = null,
            dueMin = null,
            dueMax = null,
        )
        verify(tasksApi).insert(taskListId = "remoteListId", task = localParentTaskAsRemote, parentTaskId = null)
        verifyNoMoreInteractions(tasksApi)
        verify(taskDao).getLocalOnlyTasks(localListId)
        verifyNoMoreInteractions(taskDao)
    }

    //endregion

    //region Clean-up

    @Test
    fun `when cleanup remote listing fails then sync should do nothing`() = runTest {
        // Given
        stubRemoteTaskListsFailure()

        // When
        repository.cleanStaleTasks(null)

        // Then
        verify(taskListsApi).list(maxResults = 100, null)
        verifyNoMoreInteractions(taskListsApi)
        verifyNoInteractions(taskListDao, taskDao, tasksApi, nowProvider)
    }

    @Test
    fun `when null remote list is provided then remote lists are fetch`() = runTest {
        // Given
        stubNoRemoteTaskLists()

        // When
        repository.cleanStaleTasks(null)

        // Then
        verify(taskListsApi).list(maxResults = 100, null)
        verifyNoMoreInteractions(taskListsApi)
        verify(taskListDao).deleteStaleTaskLists(emptyList())
        verifyNoMoreInteractions(taskListDao)
        verifyNoInteractions(taskDao, tasksApi, nowProvider)
    }

    @Test
    fun `when remote lists then any local list not matching id is removed`() = runTest {
        // Given
        whenever(taskListDao.getByRemoteId("remoteId")).thenReturn(null)

        // When
        val remoteTaskList = mock<RemoteTaskList> {
            on { id } doReturn "remoteId"
        }
        repository.cleanStaleTasks(listOf(remoteTaskList))

        // Then
        verify(taskListDao).deleteStaleTaskLists(listOf("remoteId"))
        verify(taskListDao).getByRemoteId("remoteId")
        verifyNoMoreInteractions(taskListDao)
        verifyNoInteractions(taskDao, taskListsApi, tasksApi, nowProvider)
    }

    @Test
    fun `when no more remote tasks then any local task with remote counterpart is removed`() = runTest {
        // Given
        stubRemoteTaskList(remoteId = "remoteId")
        val localList = mock<LocalTaskList> {
            on { id } doReturn 42
        }
        whenever(taskListDao.getByRemoteId("remoteId")).thenReturn(localList)
        val response = mockRemoteTasksResponse(remoteTasks = emptyArray())
        whenever(
            tasksApi.list(
                taskListId = "remoteId",
                showDeleted = true,
                showHidden = true,
                showCompleted = true,
                maxResults = 100,
                updatedMin = null,
                completedMin = null,
                completedMax = null,
                dueMin = null,
                dueMax = null,
            )
        ).thenReturn(response)
        val remoteTaskList = mock<RemoteTaskList> {
            on { id } doReturn "remoteId"
        }

        // When
        repository.cleanStaleTasks(listOf(remoteTaskList))

        // Then
        verify(taskListDao).deleteStaleTaskLists(listOf("remoteId"))
        verify(taskListDao).getByRemoteId("remoteId")
        verifyNoMoreInteractions(taskListDao)
        verify(taskDao).deleteStaleTasks(localList.id, emptyList())
        verifyNoMoreInteractions(taskDao)
        verify(tasksApi).list(
            taskListId = "remoteId",
            showDeleted = true,
            showHidden = true,
            showCompleted = true,
            maxResults = 100,
            updatedMin = null,
            completedMin = null,
            completedMax = null,
            dueMin = null,
            dueMax = null,
        )
        verifyNoMoreInteractions(tasksApi)
        verifyNoInteractions(taskListsApi, nowProvider)
    }

    @Test
    fun `when remote tasks then any local task not matching id is removed`() = runTest {
        // Given
        stubRemoteTaskList(remoteId = "remoteListId")
        val localList = mock<LocalTaskList> {
            on { id } doReturn 42
        }
        whenever(taskListDao.getByRemoteId("remoteListId")).thenReturn(localList)
        val remoteTask = mock<RemoteTask> {
            on { id } doReturn "remoteTaskId"
        }
        val response = mockRemoteTasksResponse(remoteTask)
        whenever(
            tasksApi.list(
                taskListId = "remoteListId",
                showDeleted = true,
                showHidden = true,
                showCompleted = true,
                maxResults = 100,
                updatedMin = null,
                completedMin = null,
                completedMax = null,
                dueMin = null,
                dueMax = null,
            )
        ).thenReturn(response)
        val remoteTaskList = mock<RemoteTaskList> {
            on { id } doReturn "remoteListId"
        }

        // When
        repository.cleanStaleTasks(listOf(remoteTaskList))

        // Then
        verify(taskListDao).deleteStaleTaskLists(listOf("remoteListId"))
        verify(taskListDao).getByRemoteId("remoteListId")
        verifyNoMoreInteractions(taskListDao)
        verify(taskDao).deleteStaleTasks(localList.id, listOf(remoteTask.id))
        verifyNoMoreInteractions(taskDao)
        verify(tasksApi).list(
            taskListId = "remoteListId",
            showDeleted = true,
            showHidden = true,
            showCompleted = true,
            maxResults = 100,
            updatedMin = null,
            completedMin = null,
            completedMax = null,
            dueMin = null,
            dueMax = null,
        )
        verifyNoMoreInteractions(tasksApi)
        verifyNoInteractions(taskListsApi, nowProvider)
    }

    //endregion
}