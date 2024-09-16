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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.opatry.google.auth.GoogleAuthenticator.OAuthToken.TokenType.Bearer
import net.opatry.google.auth.GoogleAuthenticator.OAuthToken.TokenType.Mac
import net.opatry.google.auth.GoogleAuthenticator.Permission.Books
import net.opatry.google.auth.GoogleAuthenticator.Permission.Email
import net.opatry.google.auth.GoogleAuthenticator.Permission.Keep
import net.opatry.google.auth.GoogleAuthenticator.Permission.KeepReadOnly
import net.opatry.google.auth.GoogleAuthenticator.Permission.OpenID
import net.opatry.google.auth.GoogleAuthenticator.Permission.Profile
import java.net.URLEncoder
import java.util.*
import kotlin.time.Duration.Companion.minutes

interface GoogleAuthenticator {

    /**
     * @property Profile `"https://www.googleapis.com/auth/userinfo.profile"` See your primary Google Account email address
     * @property Email `"https://www.googleapis.com/auth/userinfo.email"` See your personal info, including any personal info you've made publicly available
     * @property OpenID `"openid"` Associate you with your personal info on Google
     * @property Books Manage your books
     * @property Keep See, edit, create and permanently delete all your Google Keep data
     * @property KeepReadOnly View all your Google Keep data
     */
    enum class Permission(val scope: String) {
        Profile("https://www.googleapis.com/auth/userinfo.profile"),
        Email("https://www.googleapis.com/auth/userinfo.email"),
        OpenID("openid"),
        Books("https://www.googleapis.com/auth/books"),
        Keep("https://www.googleapis.com/auth/keep"),
        KeepReadOnly("https://www.googleapis.com/auth/keep.readonly"),
        Tasks("https://www.googleapis.com/auth/tasks"),
        TasksReadOnly("https://www.googleapis.com/auth/tasks.readonly"),
        CustomSearch("https://www.googleapis.com/auth/cse"),
    }

    @Serializable
    /**
     * @property accessToken The token that your application sends to authorize a Google API request.
     * @property expiresIn The remaining lifetime of the access token in seconds.
     * @property idToken **Note:** This property is only returned if your request included an identity scope, such as  `openid`, `profile`, or `email`. The value is a JSON Web Token (JWT) that contains digitally signed identity information about the user.
     * @property refreshToken A token that you can use to obtain a new access token. Refresh tokens are valid until the user revokes access. Note that refresh tokens are always returned for installed applications.
     * @property scope The scopes of access granted by the [accessToken] expressed as a list of [Permission].
     * @property tokenType The type of token returned. At this time, this field's value is always set to `Bearer`.
     */
    data class OAuthToken(
        @SerialName("access_token")
        val accessToken: String,

        @SerialName("expires_in")
        val expiresIn: Long,

        @SerialName("id_token")
        val idToken: String? = null,

        @SerialName("refresh_token")
        val refreshToken: String? = null,

        @SerialName("scope")
        val scope: String,

        @SerialName("token_type")
        val tokenType: TokenType,
    ) {
        /**
         * Value is case insensitive.
         *
         * @property Bearer `"bearer"` token type defined in [RFC6750](https://datatracker.ietf.org/doc/html/rfc6750) is utilized by simply including the access token string in the request.
         * @property Mac `"mac"` token type defined in [OAuth-HTTP-MAC](https://datatracker.ietf.org/doc/html/rfc6749#ref-OAuth-HTTP-MAC) is utilized by issuing a Message Authentication Code (MAC) key together with the access token that is used to sign certain components of the HTTP requests.
         */
        enum class TokenType {

//            @SerialName("bearer")
            @SerialName("Bearer")
            Bearer,

            @SerialName("mac")
            Mac,
        }
    }

    /**
     * @param permissions Permission scope. The currently available scopes are [Permission.Profile], [Permission.Email], [Permission.OpenID].
     * @param requestUserAuthorization The URL to which to request user authorization before direction
     *
     * @return auth code
     *
     * @see Permission
     */
    suspend fun authorize(permissions: List<Permission>, requestUserAuthorization: (url: String) -> Unit): String

    /**
     * @param code The code obtained through [authorize].
     *
     * @return OAuth access token
     *
     * @see Permission
     */
    suspend fun getToken(code: String): OAuthToken
}

class HttpGoogleAuthenticator(private val config: ApplicationConfig) : GoogleAuthenticator {

    /**
     * @property redirectUrl Redirect url
     * @property clientId OAuth2 Client ID
     * @property clientSecret OAuth2 Client Secret
     */
    data class ApplicationConfig(
        val redirectUrl: String,
        val clientId: String,
        val clientSecret: String,
    )

    private companion object {
        const val GOOGLE_ACCOUNTS_ROOT_URL = "https://accounts.google.com"
    }

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            CurlUserAgent()
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                url(GOOGLE_ACCOUNTS_ROOT_URL)
            }
        }
    }

    override suspend fun authorize(
        permissions: List<GoogleAuthenticator.Permission>, requestUserAuthorization: (url: String) -> Unit
    ): String {
        val uuid = UUID.randomUUID()
        val params = mapOf(
            "client_id" to config.clientId,
            "response_type" to "code",
            "redirect_uri" to URLEncoder.encode(config.redirectUrl, Charsets.UTF_8),
            "scope" to permissions.joinToString("+", transform = {
                URLEncoder.encode(it.scope, Charsets.UTF_8)
            }),
            "state" to uuid.toString(),
        ).entries.joinToString(prefix = "?", separator = "&") {
            "${it.key}=${it.value}"
        }

        return withTimeout(5.minutes) {
            callbackFlow {
                val url = Url(config.redirectUrl)
                val server = embeddedServer(Netty, port = url.port, host = url.host) {
                    routing {
                        get(url.fullPath.takeIf(String::isNotEmpty) ?: "/") {
                            fun Parameters.require(key: String): String =
                                requireNotNull(get(key)) { "Expected '$key' query parameter not available." }

                            val queryParams = call.request.queryParameters
                            try {
                                // throw if any error query parameter if provided
                                queryParams["error"]?.let(::error)

                                val state = queryParams.require("state")
                                require(uuid == UUID.fromString(state)) { "Mismatch between expected & provided state ($state)." }
                                val authCode = queryParams.require("code")
                                val status = HttpStatusCode.OK
                                call.respond(status, "$status: Authorization accepted.")
                                send(authCode)
                                close(null)
                            } catch (e: Exception) {
                                val status = HttpStatusCode.BadRequest
                                call.respond(status, "$status: ${e.message}")
                                close(e)
                            }
                        }
                    }
                }
                server.environment.monitor.subscribe(ApplicationStarted) {
                    requestUserAuthorization("$GOOGLE_ACCOUNTS_ROOT_URL/o/oauth2/auth$params")
                }
                server.start(wait = false)
                awaitClose(server::stop)
            }.first()
        }
    }

    override suspend fun getToken(code: String): GoogleAuthenticator.OAuthToken {
        // TODO depending on grant type, consider a refresh token or a authorization code
        val response = httpClient.post("https://oauth2.googleapis.com/token") {
            parameter("client_id", config.clientId)
            parameter("client_secret", config.clientSecret)
            parameter("code", code)
//            parameter("code_verifier", TODO())
            parameter("grant_type", "authorization_code")
            parameter("redirect_uri", config.redirectUrl)
            contentType(ContentType.Application.FormUrlEncoded)
        }

        if (response.status.isSuccess()) {
            return response.body()
        } else {
            throw ClientRequestException(response, response.bodyAsText())
        }
    }
}