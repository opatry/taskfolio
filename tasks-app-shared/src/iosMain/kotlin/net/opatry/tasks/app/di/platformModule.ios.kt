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

import androidx.room.Room
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.FileCredentialsStorage
import net.opatry.tasks.data.TasksAppDatabase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun platformModule(target: String): Module = module {
    single<HttpClientEngineFactory<*>> {
        Darwin
    }

    @OptIn(ExperimentalForeignApi::class)
    single<String>(named("app_root_dir")) {
        val fileManager = NSFileManager.defaultManager
        val documentDirectoryPath = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path ?: throw IllegalStateException("Could not find document directory")

        ("$documentDirectoryPath/.taskfolio").also { appRootDirPath ->
            if (!fileManager.fileExistsAtPath(appRootDirPath)) {
                val success = fileManager.createDirectoryAtPath(
                    path = appRootDirPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
                check(success) { "Failed to create directory at $appRootDirPath" }
            }
        }
    }

    single {
        val dbFilePath = get<String>(named("app_root_dir")) + "/tasks.db"
        Room.databaseBuilder<TasksAppDatabase>(dbFilePath)
    }

    single<CredentialsStorage> {
        // TODO store in database
        val credentialsFilePath = get<String>(named("app_root_dir")) + "/google_auth_token_cache.json"
        FileCredentialsStorage(credentialsFilePath)
    }
}
