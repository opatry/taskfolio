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

package net.opatry.tasks.app.auth

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.withTimeout
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.HttpGoogleAuthenticator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.minutes


class PlayServicesGoogleAuthenticator(
    private val context: Context,
    private val config: ApplicationConfig
) : HttpGoogleAuthenticator(config) {
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
}