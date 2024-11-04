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

package net.opatry.google.batch

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import io.ktor.utils.io.core.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.opatry.google.auth.GoogleAuth
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.HttpGoogleAuthenticator
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun batchMultiPartFormDataContent(parts: List<PartData>): MultiPartFormDataContent {
    val boundary = "batch_${Clock.System.now().toEpochMilliseconds().toString().encodeBase64()}"
    val contentType = ContentType.MultiPart.Mixed.withParameter("boundary", boundary)
    return MultiPartFormDataContent(parts, boundary, contentType)
}

@Serializable
data class TokenCache(

    @SerialName("access_token")
    val accessToken: String? = null,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("expiration_time_millis")
    val expirationTimeMillis: Long = 0,
)

interface CredentialsStorage {
    suspend fun load(): TokenCache?
    suspend fun store(tokenCache: TokenCache)
}

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
val testDir = File("tests")
val credentialsFile = File(testDir, "google_auth_token_cache.json")
val credentialsStorage = FileCredentialsStorage(credentialsFile.absolutePath)
val gcpClientFilename =
    "$userHome/work/taskfolio/tasks-app-shared/src/jvmMain/composeResources/files/client_secret_191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com.json"

class RequestBatchTest {
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `eval batch http`() = runTest {
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

        if (!credentialsFile.exists()) {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5.minutes) {
                    val code = authenticator.authorize(
                        listOf(
                            GoogleAuthenticator.Scope.Profile,
                            GoogleAuthenticator.Scope("https://www.googleapis.com/auth/tasks"),
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
        }.use { httpClient ->
            val batchResponse = httpClient.post("https://tasks.googleapis.com/batch") {
                // FIXME what would be a better boundary name?
//                val boundary = "batch_${Clock.System.now().toEpochMilliseconds().toString().encodeBase64()}"
                val boundary = Uuid.random().toString()
                val contentType = ContentType.MultiPart.Mixed.withParameter("boundary", boundary)
                setBody(
                    MultiPartFormDataContent(
                        listOf(
                            PartData.FormItem(
                                value = "GET /tasks/v1/users/@me/lists HTTP/1.1",
                                dispose = {},
                                partHeaders = headersOf(HttpHeaders.ContentType, "application/http"),
                            ),
                            PartData.FormItem(
                                value = "GET /tasks/v1/users/@me/lists HTTP/1.1",
                                dispose = {},
                                partHeaders = headersOf(HttpHeaders.ContentType, "application/http"),
                            ),
                        ),
                        boundary,
                        contentType
                    )
                )
            }

            if (batchResponse.status.isSuccess()) {
                val partDataList = handleBatchResponse(batchResponse)
                partDataList.forEach { part ->
                    println((part as PartData.FormItem).value)
                }
//                val batchResponses = handleBatchResponse2(batchResponse)
//                batchResponses.forEach { response ->
//                    if (response.statusCode.isSuccess()) {
//                        println(Json.decodeFromString<ResourceListResponse<TaskList>>(response.body as String))
//                    } else {
//                        error(response.body.toString())
//                        // TODO
//                        //  throw ClientRequestException(response, response.bodyAsText())
//                    }
//                }
            }
        }
    }
}