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

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.opatry.google.auth.GoogleAuth
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.HttpGoogleAuthenticator
import net.opatry.google.tasks.HttpTaskListsApi
import net.opatry.google.tasks.HttpTasksApi
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.TasksScopes
import net.opatry.google.tasks.listAll
import net.opatry.google.tasks.model.Task
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.model.TaskDataModel
import net.opatry.tasks.data.model.TaskListDataModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FileCredentialsStorage(filepath: String) : CredentialsStorage {
    private val file: File = File(filepath)

    override suspend fun load(): TokenCache? {
        return withContext(Dispatchers.IO) {
            if (file.isFile) {
                file.readText().let(Json::decodeFromString)
            } else {
                null
            }
        }
    }

    override suspend fun store(tokenCache: TokenCache) {
        val json = Json { prettyPrint = true }
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(tokenCache))
        }
    }
}

val userHome = File(System.getProperty("user.home"))
val gcpClientFilename =
    "$userHome/work/taskfolio/tasks-app-shared/src/jvmMain/composeResources/files/client_secret_191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com.json"

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

class TaskRepositorySync2Test {

    private lateinit var taskListDao: TaskListDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskListsApi: TaskListsApi
    private lateinit var tasksApi: TasksApi
    private lateinit var repository: TaskRepository

    private fun runTaskRepositoryTest(
        test: suspend /*TestScope.*/() -> Unit
    ) = /*runTest*/ runBlocking {
        // TODO reuse DI

        val testDir = File("tests")
        val dbDir = File(testDir, "db")
        val dbFile = File(dbDir, "db/tasks.db")
        dbDir.deleteRecursively()
        val db =
            //inMemoryTasksAppDatabaseBuilder()
            Room.databaseBuilder<TasksAppDatabase>(dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                //.setQueryCoroutineContext(backgroundScope.coroutineContext)
                .build()

        try {
            val googleAuthCredentials = runBlocking {
                File(gcpClientFilename).inputStream()
                    .use { inputStream ->
                        requireNotNull(Json.decodeFromStream<GoogleAuth>(inputStream).webCredentials)
                    }
            }

            val config = HttpGoogleAuthenticator.ApplicationConfig(
                redirectUrl = googleAuthCredentials.redirectUris.first(),
                clientId = googleAuthCredentials.clientId,
                clientSecret = googleAuthCredentials.clientSecret,
                authUri = googleAuthCredentials.authUri,
                tokenUri = googleAuthCredentials.tokenUri,
            )
            val authenticator = HttpGoogleAuthenticator(config)

            val credentialsFile = File(testDir, "google_auth_token_cache.json")
            val credentialsStorage = FileCredentialsStorage(credentialsFile.absolutePath)

            if (!credentialsFile.exists()) {
                withContext(Dispatchers.Default.limitedParallelism(1)) {
                    withTimeout(5.minutes) {
                        val code = authenticator.authorize(
                            listOf(
                                GoogleAuthenticator.Scope.Profile,
                                GoogleAuthenticator.Scope(TasksScopes.Tasks),
                            )
                        ) {
                            println(it)
                        }
                        authenticator.getToken(GoogleAuthenticator.Grant.AuthorizationCode(code)).let {
                            credentialsStorage.store(
                                TokenCache(
                                    accessToken = it.accessToken,
                                    refreshToken = it.refreshToken,
                                    expirationTimeMillis = (Clock.System.now() + it.expiresIn.seconds).toEpochMilliseconds()
                                )
                            )
                        }
                    }
                }
            }

            HttpClient(CIO) {
                CurlUserAgent()
                install(ContentNegotiation) {
                    json()
                }
                install(Auth) {
                    bearer {
                        // TODO handle 401 (clear storage)
                        // TODO handle no network
                        loadTokens {
                            val tokenCache = credentialsStorage.load()
                            BearerTokens(tokenCache?.accessToken ?: "", tokenCache?.refreshToken ?: "")
                        }
                        refreshTokens {
                            val refreshToken = oldTokens?.refreshToken?.let(GoogleAuthenticator.Grant::RefreshToken) ?: return@refreshTokens oldTokens
                            val t0 = Clock.System.now()
                            // return OAuthToken might not have a refreshToken, reuse old one in such a case
                            authenticator.getToken(refreshToken)
                                .also { token ->
                                    credentialsStorage.store(
                                        TokenCache(
                                            accessToken = token.accessToken,
                                            refreshToken = token.refreshToken ?: oldTokens?.refreshToken,
                                            expirationTimeMillis = (t0 + token.expiresIn.seconds).toEpochMilliseconds()
                                        )
                                    )
                                }.let {
                                    BearerTokens(it.accessToken, it.refreshToken ?: oldTokens?.refreshToken ?: "")
                                }
                        }
                    }
                }
                defaultRequest {
                    if (url.host.isEmpty()) {
                        val defaultUrl = URLBuilder().takeFrom("https://tasks.googleapis.com")
                        url.host = defaultUrl.host
                        url.port = defaultUrl.port
                        url.protocol = defaultUrl.protocol
                        if (!url.encodedPath.startsWith('/')) {
                            val basePath = defaultUrl.encodedPath
                            url.encodedPath = "$basePath/${url.encodedPath}"
                        }
                    }
                }
            }.use { httpClient ->
                taskListDao = db.getTaskListDao()
                taskDao = db.getTaskDao()
                taskListsApi = HttpTaskListsApi(httpClient)
                tasksApi = HttpTasksApi(httpClient)
                repository = TaskRepository(taskListDao, taskDao, taskListsApi, tasksApi)

                test()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `integration test`() = runTaskRepositoryTest {
        logRepository("Repository empty", repository)

        val ltl1 = TaskListEntity(title = "Local_TaskList_1")
        taskListDao.insert(ltl1)
        val lt1_1 = TaskEntity(ltl1.id, "Local_Task_1.1", parentId = null, position = "00000000000000000000")
        val lt1_2 = TaskEntity(ltl1.id, "Local_Task_1.2", parentId = null, position = "00000000000000000001")
        val lt1_2_1 = TaskEntity(ltl1.id, "Local_SubTask_1.2.1", parentId = lt1_2.id, position = "00000000000000000000")
        val lt1_2_2 = TaskEntity(ltl1.id, "Local_SubTask_1.2.2", parentId = lt1_2.id, position = "00000000000000000001")
        val lt1_3 = TaskEntity(ltl1.id, "Local_Task_1.3", parentId = null, position = "00000000000000000002")
        taskDao.insertAll(listOf(lt1_1, lt1_2, lt1_2_1, lt1_2_2, lt1_3))

        val ltl2 = TaskListEntity(title = "Local_TaskList_2")
        taskListDao.insert(ltl2)
        val lt2_1 = TaskEntity(ltl2.id, "Local_Task_2.1", parentId = null, position = "00000000000000000000")
        val lt2_1_1 = TaskEntity(ltl2.id, "Local_SubTask_2.1.1", parentId = lt2_1.id, position = "00000000000000000000")
        val lt2_2 = TaskEntity(ltl2.id, "Local_Task_2.2", parentId = null, position = "00000000000000000001")
        taskDao.insertAll(listOf(lt2_1, lt2_1_1, lt2_2))

        logRepository("Repository local only", repository)

        println("Clean remote content")
        taskListsApi.listAll().forEachIndexed { index, taskList ->
            if (index == 0) {
                tasksApi.listAll(taskList.id, showCompleted = true, showHidden = true).forEach { task ->
                    tasksApi.delete(taskList.id, task.id)
                }
                tasksApi.clear(taskList.id)
            } else {
                taskListsApi.delete(taskList.id)
            }
        }

        logRemote("Remote content empty (except default task)", taskListsApi, tasksApi)

        println("Create remote content")
        val rtl1 = taskListsApi.insert(TaskList("Remote_TaskList_1"))
        val rt1_1 = tasksApi.insert(rtl1.id, Task("Remote_Task_1.1"), parentTaskId = null, previousTaskId = null)
        val rt1_2 = tasksApi.insert(rtl1.id, Task("Remote_Task_1.2"), parentTaskId = null, previousTaskId = rt1_1.id)
        val rt1_2_1 = tasksApi.insert(rtl1.id, Task("Remote_SubTask_1.2.1"), parentTaskId = rt1_2.id, previousTaskId = null)
        val rt1_2_2 = tasksApi.insert(rtl1.id, Task("Remote_SubTask_1.2.2"), parentTaskId = rt1_2.id, previousTaskId = rt1_2_1.id)
        val rt1_3 = tasksApi.insert(rtl1.id, Task("Remote_Task_1.3"), parentTaskId = null, previousTaskId = rt1_2.id)

        val rtl2 = taskListsApi.insert(TaskList("Remote_TaskList_2"))
        val rt2_1 = tasksApi.insert(rtl2.id, Task("Remote_Task_2.1"), parentTaskId = null, previousTaskId = null)
        val rt2_1_1 = tasksApi.insert(rtl2.id, Task("Remote_SubTask_2.1.1"), parentTaskId = rt2_1.id, previousTaskId = null)
        val rt2_2 = tasksApi.insert(rtl2.id, Task("Remote_Task_2.2"), parentTaskId = null, previousTaskId = rt2_1.id)

        logRemote("Remote content created", taskListsApi, tasksApi)

        val t0 = System.currentTimeMillis()
        println("Sync started…")
        repository.sync()
        println("Sync ended in ${System.currentTimeMillis() - t0}ms\n")
        logRepository("Repository after sync", repository)
        logRemote("Remote content after sync", taskListsApi, tasksApi)

        val taskLists = repository.getTaskLists().firstOrNull()
            ?: error("Task lists not found")
        assertEquals(5, taskLists.size)
        val fltl1 = taskLists.find { it._entity.id == ltl1.id }
            ?: error("final local task list 1 not found")
        val fltl2 = taskLists.find { it._entity.id == ltl2.id }
            ?: error("final local task list 2 not found")
        assertNotNull(fltl1._entity.remoteId)
        fltl1.tasks.find { it._entity.id == lt1_1.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl1.id, it._entity.parentListLocalId)
            assertNull(it._entity.parentTaskLocalId)
            assertNull(it._entity.parentTaskRemoteId)
        } ?: error("final local task 1.1 not found")
        fltl1.tasks.find { it._entity.id == lt1_2.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl1.id, it._entity.parentListLocalId)
            assertNull(it._entity.parentTaskLocalId)
            assertNull(it._entity.parentTaskRemoteId)
        } ?: error("final local task 1.2 not found")
        fltl1.tasks.find { it._entity.id == lt1_2_1.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl1.id, it._entity.parentListLocalId)
            assertEquals(lt1_2.id, it._entity.parentTaskLocalId)
            assertEquals(rt1_2.id, it._entity.parentTaskRemoteId)
        } ?: error("final local task 1.2.1 not found")
        fltl1.tasks.find { it._entity.id == lt1_2_2.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl1.id, it._entity.parentListLocalId)
            assertEquals(lt1_2.id, it._entity.parentTaskLocalId)
            assertEquals(rt1_2.id, it._entity.parentTaskRemoteId)
        } ?: error("final local task 1.2.2 not found")
        fltl1.tasks.find { it._entity.id == lt1_3.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl1.id, it._entity.parentListLocalId)
            assertNull(it._entity.parentTaskLocalId)
            assertNull(it._entity.parentTaskRemoteId)
        } ?: error("final local task 1.3 not found")
        assertNotNull(fltl2._entity.remoteId)
        fltl2.tasks.find { it._entity.id == lt2_1.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl2.id, it._entity.parentListLocalId)
            assertNull(it._entity.parentTaskLocalId)
            assertNull(it._entity.parentTaskRemoteId)
        } ?: error("final local task 2.1 not found")
        fltl2.tasks.find { it._entity.id == lt2_1_1.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl2.id, it._entity.parentListLocalId)
            assertEquals(lt2_1.id, it._entity.parentTaskLocalId)
        } ?: error("final local task 2.1.1 not found")
        fltl2.tasks.find { it._entity.id == lt2_2.id }?.let {
            assertNotNull(it._entity.remoteId)
            assertEquals(ltl2.id, it._entity.parentListLocalId)
            assertNull(it._entity.parentTaskLocalId)
            assertNull(it._entity.parentTaskRemoteId)
        } ?: error("final local task 2.2 not found")

        val frtl1 = taskLists.find { it._entity.remoteId == rtl1.id }
            ?: error("final remote task list 1 not found")
        val frtl2 = taskLists.find { it._entity.remoteId == rtl2.id }
            ?: error("final remote task list 2 not found")
        assertNotNull(frtl1.tasks.find { it._entity.remoteId == rt1_1.id }?._entity?.remoteId)
        assertNotNull(frtl1.tasks.find { it._entity.remoteId == rt1_2.id }?._entity?.remoteId)
        assertNotNull(frtl1.tasks.find { it._entity.remoteId == rt1_2_1.id }?._entity?.remoteId)
        assertNotNull(frtl1.tasks.find { it._entity.remoteId == rt1_2_2.id }?._entity?.remoteId)
        assertNotNull(frtl1.tasks.find { it._entity.remoteId == rt1_3.id }?._entity?.remoteId)
        assertNotNull(frtl2._entity.remoteId)
        assertNotNull(frtl2.tasks.find { it._entity.remoteId == rt2_1.id }?._entity?.remoteId)
        assertNotNull(frtl2.tasks.find { it._entity.remoteId == rt2_1_1.id }?._entity?.remoteId)
        assertNotNull(frtl2.tasks.find { it._entity.remoteId == rt2_2.id }?._entity?.remoteId)

        // TODO check local & remote parenting (parent list & parent task)
    }
}

private var taskListId = 1L
private fun TaskListEntity(title: String): TaskListEntity {
    return TaskListEntity(id = taskListId++, title = title, lastUpdateDate = Clock.System.now())
}

private var taskId = 1L
private fun TaskEntity(listId: Long, title: String, parentId: Long?, position: String): TaskEntity {
    return TaskEntity(
        id = taskId++,
        parentListLocalId = listId,
        parentTaskLocalId = parentId,
        title = title,
        lastUpdateDate = Clock.System.now(),
        position = position
    )
}

suspend fun logRepository(label: String, repository: TaskRepository) {
    println("-----------------------------")
    println(" 💾 $label")
    println("-----------------------------")
    repository.getTaskLists().firstOrNull()?.forEach { taskList ->
        println(taskList.stringRepresentation)
        taskList.tasks.forEach { task ->
            println(task.stringRepresentation)
        }
    }
    println("")
}

val TaskListDataModel.isRemote: Boolean
    get() = _entity.remoteId != null
val TaskListDataModel.isLocalOnly: Boolean
    get() = _entity.remoteId == null

val TaskDataModel.isRemote: Boolean
    get() = _entity.remoteId != null
val TaskDataModel.isLocalOnly: Boolean
    get() = _entity.remoteId == null

val TaskListDataModel.stringRepresentation: String
    get() = buildString {
        append(title)
        append(" [$id] {${_entity.remoteId}} ")
        if (isRemote) append("☁️") else append(" ")
        append(" ")
        if (isLocalOnly) append("💾") else append(" ")
    }

val TaskDataModel.stringRepresentation: String
    get() = buildString {
        if (_entity.parentTaskLocalId != null || _entity.parentTaskRemoteId != null) append("    ") else append("  ")
        append(title)
        append(" [$id] {${_entity.remoteId}} ")
        if (isRemote) append("☁️") else append(" ")
        append(" ")
        if (isLocalOnly) append("💾") else append(" ")
        append(" ")
        append(_entity.position)
    }

suspend fun logRemote(label: String, taskListsApi: TaskListsApi, tasksApi: TasksApi) {
    println("*****************************")
    println(" ☁️ $label")
    println("*****************************")
    taskListsApi.listAll().forEach { taskList ->
        println(taskList.stringRepresentation)
        val tasks = tasksApi.listAll(taskList.id, showCompleted = true, showHidden = true, showDeleted = true)
        val (parentTasks, subTasks) = tasks.partition { it.parent == null }
        parentTasks.sortedBy(Task::position).forEach { task ->
            println(task.stringRepresentation)
            subTasks.filter { it.parent == task.id }.sortedBy(Task::position).forEach { subTask ->
                println(subTask.stringRepresentation)
            }
        }
    }
    println("")
}

val TaskList.stringRepresentation: String
    get() = buildString {
        append(title)
        append(" {$id} ")
    }

val Task.stringRepresentation: String
    get() = buildString {
        if (parent != null) append("    ") else append("  ")
        append(title)
        append(" {$id} ")
        append(" ")
        append(position)
        if (isCompleted) append(" ✅") else append("    ")
        if (isDeleted) append(" 🗑️ ") else append("    ")
    }