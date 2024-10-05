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

package net.opatry.google.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.fullPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.net.URLEncoder
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import io.ktor.client.engine.cio.CIO as ClientEngineCIO
import io.ktor.server.cio.CIO as ServerEngineCIO

/**
 * A Google OAuth2 authenticator using a localhost HTTP server for redirect URL interception and
 * HTTP client for auth & token requests.
 */
open class HttpGoogleAuthenticator(private val config: ApplicationConfig) : GoogleAuthenticator {

    /**
     * @property redirectUrl Redirect url
     * @property clientId OAuth2 Client ID
     * @property clientSecret OAuth2 Client Secret
     */
    data class ApplicationConfig(
        val redirectUrl: String,
        val clientId: String,
        val clientSecret: String,
        val authUri: String,
        val tokenUri: String,
    )

    private val httpClient: HttpClient by lazy {
        HttpClient(ClientEngineCIO) {
            CurlUserAgent()
            install(ContentNegotiation) {
                json()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun authorize(
        scopes: List<GoogleAuthenticator.Scope>,
        force: Boolean,
        requestUserAuthorization: (url: Any) -> Unit
    ): String {
        // FIXME URLEncoder is not KMP, maybe ktor has something?
        val uuid = Uuid.random()
        val params = buildMap {
            put("client_id", config.clientId)
            put("response_type", "code")
            put("redirect_uri", URLEncoder.encode(config.redirectUrl, Charsets.UTF_8.name()))
            put("scope", scopes.joinToString("+") {
                URLEncoder.encode(it.value, Charsets.UTF_8.name())
            })
            put("state", uuid.toString())
            // to get a refresh token, need to request consent & offline access
            if (force) {
                put("prompt", "consent")
                put("access_type", "offline")
            }
        }.entries.joinToString(prefix = "?", separator = "&") {
            "${it.key}=${it.value}"
        }

        return withTimeout(5.minutes) {
            callbackFlow {
                val url = Url(config.redirectUrl)
                val server = embeddedServer(ServerEngineCIO, port = url.port, host = url.host) {
                    routing {
                        get("signed-in") {
                            val status = HttpStatusCode.OK
                            call.respond(status, "$status: Authorization accepted.")
                        }
                        get("error") {
                            val errorMessage = call.request.queryParameters["message"] ?: "Unknown error"
                            val status = HttpStatusCode.BadRequest
                            call.respond(status, "$status: $errorMessage")
                        }
                        get(url.fullPath.takeIf(String::isNotEmpty) ?: "/") {
                            fun Parameters.require(key: String): String =
                                requireNotNull(get(key)) { "Expected '$key' query parameter not available." }

                            val queryParams = call.request.queryParameters
                            try {
                                // throw if any error query parameter if provided
                                queryParams["error"]?.let(::error)

                                val state = queryParams.require("state")
                                require(uuid == Uuid.parse(state)) { "Mismatch between expected & provided state ($state)." }
                                val authCode = queryParams.require("code")
                                // redirect to another endpoint to hide the code from the user as quickly as possible
                                call.respondRedirect("${url}/signed-in")
                                send(authCode)
                                close(null)
                            } catch (e: Exception) {
                                call.respondRedirect("${url}/error?message=${e.message}")
                                close(e)
                            }
                        }
                    }
                }
                server.monitor.subscribe(ApplicationStarted) {
                    requestUserAuthorization("${config.authUri}$params")
                }
                server.start(wait = false)
                awaitClose(server::stop)
            }.first()
        }
    }

    override suspend fun getToken(grant: GoogleAuthenticator.Grant): GoogleAuthenticator.OAuthToken {
        val response = httpClient.post(config.tokenUri) {
            parameter("client_id", config.clientId)
            parameter("client_secret", config.clientSecret)
            parameter("grant_type", grant.type)
            when (grant) {
                is GoogleAuthenticator.Grant.AuthorizationCode -> {
                    parameter("code", grant.code)
                    // TODO PKCE "code_verifier"
                    parameter("redirect_uri", config.redirectUrl)
                }

                is GoogleAuthenticator.Grant.RefreshToken -> {
                    parameter("refresh_token", grant.refreshToken)
                }
            }
            contentType(ContentType.Application.FormUrlEncoded)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }
}