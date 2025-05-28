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

package net.opatry.google.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.fullPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.html.a
import kotlinx.html.code
import kotlinx.html.details
import kotlinx.html.pre
import kotlinx.html.summary
import net.opatry.google.auth.html.authScaffold
import net.opatry.google.auth.html.internalError
import net.opatry.google.auth.html.notFound
import java.net.URLDecoder
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
class HttpGoogleAuthenticator(private val config: ApplicationConfig) : GoogleAuthenticator {

    /**
     * @property redirectUrl Redirect url
     * @property clientId OAuth2 Client ID
     * @property clientSecret OAuth2 Client Secret
     * @property authUri OAuth2 Authorization URI
     * @property tokenUri OAuth2 Token URI
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
            expectSuccess = true
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

        var authCode: String? = null

        return withTimeout(5.minutes) {
            callbackFlow {
                val url = Url(config.redirectUrl)
                val server = embeddedServer(ServerEngineCIO, port = url.port, host = url.host) {
                    install(StatusPages) {
                        notFound()
                        internalError()
                    }

                    routing {
                        get("signed-in") {
                            // FIXME redirect URL coupled to product
                            val redirectToAppUrl = "taskfolio://signin/success"
                            call.respondHtml(HttpStatusCode.OK) {
                                authScaffold(
                                    pageTitle = "Google Authorization",
                                    subTitle = "Google Authorization successful",
                                    illustrationName = "undraw_completing_re_i7ap",
                                    redirectUrl = redirectToAppUrl
                                ) {
                                    +"You can "
                                    a(href = redirectToAppUrl) {
                                        +" go back to the application"
                                    }
                                    +" and close this window."
                                }
                            }

                            authCode?.let {
                                send(it)
                                close(null)
                            } ?: close(IllegalStateException("No auth code"))
                        }
                        get("error") {
                            val errorMessage = call.request.queryParameters["message"] ?: "Unknown error"
                            val errorDetails = call.request.queryParameters["details"]
                            // FIXME redirect URL coupled to product
                            val redirectToAppUrl = "taskfolio://signin/failure?message=${errorMessage}"
                            call.respondHtml(HttpStatusCode.BadRequest) {
                                authScaffold(
                                    pageTitle = "Google Authorization",
                                    subTitle = "Google Authorization failed",
                                    illustrationName = "undraw_warning_re_eoyh",
                                    redirectUrl = redirectToAppUrl
                                ) {
                                    +"An error occurred ("
                                    code {
                                        +URLDecoder.decode(errorMessage, Charsets.UTF_8.name())
                                    }
                                    +") during the Google Authorization process. Please try again."

                                    if (errorDetails != null) {
                                        details {
                                            summary {
                                                +"See details"
                                            }
                                            pre {
                                                +URLDecoder.decode(errorDetails, Charsets.UTF_8.name())
                                            }
                                        }
                                    }
                                }
                            }
                            close(IllegalStateException(errorMessage))
                        }
                        get(url.fullPath.takeIf(String::isNotEmpty) ?: "/") {
                            fun Parameters.require(key: String): String {
                                return requireNotNull(get(key)) { "Expected '$key' query parameter not available." }
                            }

                            val queryParams = call.request.queryParameters
                            try {
                                // throw if any error query parameter if provided
                                queryParams["error"]?.let(::error)

                                val state = queryParams.require("state")
                                require(uuid == Uuid.parse(state)) { "Mismatch between expected & provided state ($state)." }
                                // store the auth code in memory for further reuse in /signed-in route
                                // redirect immediately minimizes the time the code is visible to the user in the URL
                                authCode = queryParams.require("code")
                                // redirect to another endpoint to hide the code from the user as quickly as possible
                                call.respondRedirect("${url}/signed-in")
                            } catch (e: Exception) {
                                // FIXME URLEncoder is not KMP-friendly
                                val errorQueryParams = mapOf(
                                    "message" to e.message,
                                    "details" to e.stackTraceToString(),
                                ).entries.joinToString(prefix = "?", separator = "&") { (key, value) ->
                                    "${URLEncoder.encode(key, Charsets.UTF_8.name())}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
                                }
                                call.respondRedirect("${url}/error$errorQueryParams")
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

        return response.body()
    }
}