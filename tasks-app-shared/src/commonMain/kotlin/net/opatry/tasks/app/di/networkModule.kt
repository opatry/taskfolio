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

package net.opatry.tasks.app.di

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.ClientRequestExceptionConverterFactory
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.TasksApi
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds


enum class HttpClientName {
    Tasks,
}

val networkModule = module {
    single(named(HttpClientName.Tasks)) {
        val credentialsStorage = get<CredentialsStorage>()

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
        }
    }

    single {
        Ktorfit.Builder()
            .httpClient(get<HttpClient>(named(HttpClientName.Tasks)))
            .baseUrl("https://tasks.googleapis.com/")
            .converterFactories(ClientRequestExceptionConverterFactory)
            .build()
    }

    single {
        // FIXME why ktorfit.createTaskListsApi() isn't generated?
        //  It appears to be generated in jvmMain build dir of :google:tasks module, not being reachable from :tasks-app-shared
        get<Ktorfit>().create<TaskListsApi>()
    }

    single {
        // FIXME why ktorfit.createTasksApi() isn't generated?
        //  It appears to be generated in jvmMain build dir of :google:tasks module, not being reachable from :tasks-app-shared
        get<Ktorfit>().create<TasksApi>()
    }
}