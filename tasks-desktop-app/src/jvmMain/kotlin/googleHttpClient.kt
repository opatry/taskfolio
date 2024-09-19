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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.opatry.google.auth.GoogleAuth
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.HttpGoogleAuthenticator
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.FileCredentialsStorage
import net.opatry.tasks.TokenCache
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
suspend fun getGoogleAuthToken(credentialsFilename: String, scope: List<GoogleAuthenticator.Permission>, onAuth: (url: String) -> Unit): Pair<String?, String?> {
    val tokenCacheFile = File("google_auth_token_cache.json")
    val credentialsStorage: CredentialsStorage = FileCredentialsStorage(tokenCacheFile.absolutePath)
    val tokenCache = credentialsStorage.load()?.takeIf { it.expirationTimeMillis > System.currentTimeMillis() }
    return tokenCache?.let { it.accessToken to it.refreshToken } ?: run {
        val googleAuthCredentials = ClassLoader.getSystemResourceAsStream(credentialsFilename)?.let { inputStream ->
            Json.decodeFromStream<GoogleAuth>(inputStream).credentials
        } ?: error("Failed to load Google Auth credentials $credentialsFilename")
        val config = HttpGoogleAuthenticator.ApplicationConfig(
            redirectUrl = googleAuthCredentials.redirectUris.first(),
            clientId = googleAuthCredentials.clientId,
            clientSecret = googleAuthCredentials.clientSecret,
        )
        val t0 = System.currentTimeMillis()
        val googleAuthenticator: GoogleAuthenticator = HttpGoogleAuthenticator(config)
        val code = googleAuthenticator.authorize(scope, onAuth)
        val token = googleAuthenticator.getToken(code)
        credentialsStorage.store(
            TokenCache(
                token.accessToken,
                token.refreshToken,
                t0 + token.expiresIn.seconds.inWholeMilliseconds
            )
        )
        token.accessToken to token.refreshToken
    }
}

suspend fun buildGoogleHttpClient(serviceUrl: String, credentialsFilename: String, scope: List<GoogleAuthenticator.Permission>, onAuth: (url: String) -> Unit): HttpClient {
    val (accessToken, refreshToken) = getGoogleAuthToken(credentialsFilename, scope, onAuth)
    return HttpClient(CIO) {
        CurlUserAgent()
        install(ContentNegotiation) {
            json()
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken ?: "", refreshToken ?: "")
                }
            }
        }
        defaultRequest {
            if (url.host.isEmpty()) {
                val defaultUrl = URLBuilder().takeFrom(serviceUrl)
                url.host = defaultUrl.host
                url.protocol = defaultUrl.protocol
                if (!url.encodedPath.startsWith('/')) {
                    val basePath = defaultUrl.encodedPath
                    url.encodedPath = "$basePath/${url.encodedPath}"
                }
            }
        }
    }
}