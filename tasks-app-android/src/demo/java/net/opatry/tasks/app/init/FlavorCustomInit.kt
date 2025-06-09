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

package net.opatry.tasks.app.init

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import net.opatry.tasks.app.R
import net.opatry.tasks.data.TaskDao
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.UserDao
import net.opatry.tasks.data.entity.TaskEntity
import net.opatry.tasks.data.entity.TaskListEntity
import net.opatry.tasks.data.entity.UserEntity
import org.koin.android.ext.android.get
import kotlin.time.Duration.Companion.days

object FlavorCustomInit {
    fun Application.init() {
        // TODO would be more elegant to use Room preloaded data mechanism.
        //  Was tested but didn't work first time, not investigated a lot.
        //  That being said, it's simpler to make evolution on pre-filled content programmatically.
        //  see https://developer.android.com/training/data-storage/room/prepopulate
        val userDao = get<UserDao>()
        val taskListDao = get<TaskListDao>()
        val taskDao = get<TaskDao>()
        runBlocking {
            withContext(Dispatchers.Default) {
                userDao.setSignedInUser(
                    UserEntity(
                        remoteId = "demo",
                        email = "jane.do@acme.org",
                        name = "Jane Doe",
                        avatarUrl = "file:///android_asset/avatar.png",
                        isSignedIn = true,
                    )
                )
                val myTasksListId = taskListDao.insert(
                    TaskListEntity(
                        title = getString(R.string.demo_task_list_default),
                        lastUpdateDate = Clock.System.now(),
                        sorting = TaskListEntity.Sorting.DueDate,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "my_task1",
                        title = getString(R.string.demo_task_list_default_task1),
                        notes = getString(R.string.demo_task_list_default_task1_notes),
                        parentListLocalId = myTasksListId,
                        dueDate = Clock.System.now() + 1.days,
                        lastUpdateDate = Clock.System.now(),
                        position = "1",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "my_task2",
                        title = getString(R.string.demo_task_list_default_task2),
                        parentListLocalId = myTasksListId,
                        dueDate = Clock.System.now() - 1.days,
                        lastUpdateDate = Clock.System.now(),
                        position = "2",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "my_task3",
                        title = getString(R.string.demo_task_list_default_task3),
                        notes = getString(R.string.demo_task_list_default_task3_notes),
                        parentListLocalId = myTasksListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "3",
                        isCompleted = true,
                        completionDate = Clock.System.now() - 1.days,
                    )
                )
                val groceriesListId = taskListDao.insert(
                    TaskListEntity(
                        title = getString(R.string.demo_task_list_groceries),
                        lastUpdateDate = Clock.System.now(),
                        sorting = TaskListEntity.Sorting.Title,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "groceries_task1",
                        title = getString(R.string.demo_task_list_groceries_task1),
                        parentListLocalId = groceriesListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "1",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "groceries_task2",
                        title = getString(R.string.demo_task_list_groceries_task2),
                        parentListLocalId = groceriesListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "2",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "groceries_task3",
                        title = getString(R.string.demo_task_list_groceries_task3),
                        parentListLocalId = groceriesListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "3",
                        isCompleted = true,
                        completionDate = Clock.System.now() - 2.days,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "groceries_task4",
                        title = getString(R.string.demo_task_list_groceries_task4),
                        parentListLocalId = groceriesListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "4",
                        isCompleted = true,
                        completionDate = Clock.System.now(),
                    )
                )
                val homeListId = taskListDao.insert(
                    TaskListEntity(
                        title = getString(R.string.demo_task_list_home),
                        lastUpdateDate = Clock.System.now(),
                        sorting = TaskListEntity.Sorting.UserDefined,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "home_task1",
                        title = getString(R.string.demo_task_list_home_task1),
                        parentListLocalId = homeListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "1",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "home_task2",
                        title = getString(R.string.demo_task_list_home_task2),
                        parentListLocalId = homeListId,
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "2",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "home_task3",
                        title = getString(R.string.demo_task_list_home_task3),
                        notes = getString(R.string.demo_task_list_home_task3_notes),
                        parentListLocalId = homeListId,
                        dueDate = Clock.System.now() + 1.days,
                        lastUpdateDate = Clock.System.now(),
                        position = "3",
                        isCompleted = false,
                    )
                )
                val workListId = taskListDao.insert(
                    TaskListEntity(
                        title = getString(R.string.demo_task_list_work),
                        lastUpdateDate = Clock.System.now(),
                        sorting = TaskListEntity.Sorting.UserDefined,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "work_task1",
                        title = getString(R.string.demo_task_list_work_task1),
                        notes = getString(R.string.demo_task_list_work_task1_notes),
                        parentListLocalId = workListId,
                        dueDate = Clock.System.now() - 1.days,
                        lastUpdateDate = Clock.System.now(),
                        position = "1",
                        isCompleted = false,
                    )
                )
                val teamMeetingTaskId = taskDao.insert(
                    TaskEntity(
                        remoteId = "work_task2",
                        title = getString(R.string.demo_task_list_work_task2),
                        notes = getString(R.string.demo_task_list_work_task2_notes),
                        parentListLocalId = workListId,
                        dueDate = Clock.System.now(),
                        lastUpdateDate = Clock.System.now(),
                        position = "2",
                        isCompleted = false,
                    )
                )
                taskDao.insert(
                    TaskEntity(
                        remoteId = "work_task3",
                        title = getString(R.string.demo_task_list_work_task3),
                        notes = getString(R.string.demo_task_list_work_task3_notes),
                        parentListLocalId = workListId,
                        parentTaskLocalId = teamMeetingTaskId,
                        parentTaskRemoteId = "work_task2",
                        dueDate = null,
                        lastUpdateDate = Clock.System.now(),
                        position = "1",
                        isCompleted = false,
                    )
                )
            }
        }
    }
}