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
package net.opatry.tasks.app.di

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import net.opatry.google.profile.UserInfoApi
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.logging.Logger
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.NowProvider
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.presentation.UserViewModel
import net.opatry.tasks.data.TaskDao
import net.opatry.tasks.data.TaskListDao
import net.opatry.tasks.data.TaskRepository
import net.opatry.tasks.data.UserDao
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.time.Duration


@OptIn(KoinExperimentalAPI::class)
class DesktopDITest {
    @Test
    fun `verify all modules`() {
        val allModules = module {
            includes(
                utilModule,
                loggingModule,
                platformModule(),
                dataModule,
                authModule("some_id"),
                networkModule,
                tasksAppModule,
            )
        }
        allModules.verify(
            injections = injectedParameters(
                definition<HttpClient>(HttpClientEngine::class, HttpClientConfig::class),
                definition<TaskListsViewModel>(Duration::class),
                definition<File>(URI::class),
                definition<UserViewModel>(UserDao::class, CredentialsStorage::class, UserInfoApi::class),
            )
        )
    }

    @Test
    fun `verify logging module`() {
        loggingModule.verify()
    }

    @Test
    fun `verify platform module`() {
        platformModule().verify(
            injections = injectedParameters(
                definition<File>(URI::class),
            )
        )
    }

    @Test
    fun `verify data module`() {
        dataModule.verify()
    }

    @Test
    fun `verify auth module`() {
        authModule("some_id").verify()
    }

    @Test
    fun `verify network module`() {
        networkModule.verify(
            injections = injectedParameters(
                definition<HttpClient>(HttpClientEngine::class, HttpClientConfig::class),
            )
        )
    }

    @Test
    fun `verify app module`() {
        tasksAppModule.verify(
            injections = injectedParameters(
                definition<TaskRepository>(TaskListDao::class, TaskDao::class, TaskListsApi::class, TasksApi::class, NowProvider::class),
                definition<TaskListsViewModel>(Duration::class, Logger::class),
                definition<UserViewModel>(UserDao::class, CredentialsStorage::class, UserInfoApi::class, NowProvider::class),
            )
        )
    }
}