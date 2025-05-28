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

package net.opatry.tasks.app.auth

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withTimeout
import net.opatry.google.auth.GoogleAuthenticator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.minutes


class PlayServicesGoogleAuthenticator(
    private val context: Context,
    private val config: ApplicationConfig
) : GoogleAuthenticator {

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
        HttpClient(CIO) {
            CurlUserAgent()
            install(ContentNegotiation) {
                json()
            }
            expectSuccess = true
        }
    }

    override suspend fun authorize(
        scopes: List<GoogleAuthenticator.Scope>,
        force: Boolean,
        requestUserAuthorization: (authorizationRequest: Any) -> Unit
    ): String {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map { Scope(it.value) })
            .requestOfflineAccess(config.clientId, force)
            .build()

        return withTimeout(5.minutes) {
            suspendCoroutine { continuation ->
                Identity.getAuthorizationClient(context)
                    .authorize(authorizationRequest)
                    .addOnSuccessListener { result ->
                        if (result.hasResolution()) {
                            requestUserAuthorization(result)
                            continuation.resume("")
                        } else {
                            result.serverAuthCode?.let { authCode ->
                                continuation.resume(authCode)
                            } ?: run {
                                continuation.resumeWithException(IllegalStateException("No server auth code"))
                            }
                        }
                    }.addOnFailureListener(continuation::resumeWithException)
            }
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