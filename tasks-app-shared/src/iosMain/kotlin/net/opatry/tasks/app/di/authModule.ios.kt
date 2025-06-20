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

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.GoogleAuthenticator.OAuthToken.TokenType
import net.opatry.tasks.TokenCache
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import kotlin.time.Duration.Companion.milliseconds

actual fun authModule(gcpClientId: String): Module = module {
    single<GoogleAuthenticator> {
        // TODO implement GoogleAuthenticator for iOS
        object : GoogleAuthenticator {
            override suspend fun authorize(
                scopes: List<GoogleAuthenticator.Scope>,
                force: Boolean,
                requestUserAuthorization: (Any) -> Unit
            ): String = ""

            @OptIn(
                BetaInteropApi::class,
                ExperimentalForeignApi::class,
            )
            override suspend fun getToken(grant: GoogleAuthenticator.Grant): GoogleAuthenticator.OAuthToken {
                val now = Clock.System.now().toEpochMilliseconds()
                val fileManager = NSFileManager.defaultManager
                val documentDirectoryPath = fileManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                )?.path ?: throw IllegalStateException("Could not find document directory")
                val credentialsFilePath = "$documentDirectoryPath/hacky_token_cache.json"

                require(fileManager.fileExistsAtPath(credentialsFilePath)) {
                    "Credentials file does not exist at path: $credentialsFilePath"
                }

                val data = NSData.dataWithContentsOfFile(credentialsFilePath)
                    ?: error("Failed to load data from $credentialsFilePath")

                val content = NSString.create(data, NSUTF8StringEncoding)?.toString()
                    ?: error("Failed to convert data to string from $credentialsFilePath")

                return Json.decodeFromString<TokenCache>(content).let {
                    GoogleAuthenticator.OAuthToken(
                        accessToken = it.accessToken ?: "",
                        expiresIn = (it.expirationTimeMillis - now).milliseconds.inWholeSeconds,
                        idToken = null,
                        refreshToken = it.refreshToken,
                        scope = "", // TODO
                        tokenType = TokenType.Bearer,
                    )
                }
            }
        }
    }
}