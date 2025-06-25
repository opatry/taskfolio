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
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.HttpTaskListsApi
import net.opatry.google.tasks.HttpTasksApi
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.google.tasks.TasksApiHttpResponseValidator
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ro.cosminmihu.ktor.monitor.ContentLength
import ro.cosminmihu.ktor.monitor.KtorMonitorLogging
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import net.opatry.logging.Logger as TaskfolioLogger


enum class HttpClientName {
    Tasks,
}

val networkModule = module {
    single(named(HttpClientName.Tasks)) {
        val credentialsStorage = get<CredentialsStorage>()

        HttpClient(get<HttpClientEngineFactory<*>>()) {
            // It appears Google Tasks require the user agent to contain the "gzip" string
            // to accept gzip encoding in combination of `Accept-Encoding: gzip`.
            // see https://developers.google.com/tasks/performance#gzip
            // When tried without, keeping original user agent, it works, so stick to standard.
            CurlUserAgent()
            install(ContentNegotiation) {
                json()
            }
            expectSuccess = true
            HttpResponseValidator(TasksApiHttpResponseValidator("tasks.googleapis.com"))
            install(ContentEncoding) {
                gzip()
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    val taskfolioLogger = get<TaskfolioLogger>()
                    override fun log(message: String) {
                        taskfolioLogger.logInfo(message)
                    }
                }
            }
            install(KtorMonitorLogging) {
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
                showNotification = true
                retentionPeriod = 1.hours
                maxContentLength = ContentLength.Default
            }
            install(HttpCache)
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
                        val authenticator = get<GoogleAuthenticator>()
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
        }
    }

    single<TaskListsApi> {
        HttpTaskListsApi(get(named(HttpClientName.Tasks)))
    }

    single<TasksApi> {
        HttpTasksApi(get(named(HttpClientName.Tasks)))
    }
}