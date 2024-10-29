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

package net.opatry.tasks.app.ui.component

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TasksScopes
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.onboarding_screen_authorize_cta
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@Composable
actual fun AuthorizeGoogleTasksButton(
    modifier: Modifier,
    onSuccess: (GoogleAuthenticator.OAuthToken
) -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val authenticator = koinInject<GoogleAuthenticator>()
    var ongoingAuth by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val startForResult = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
            val authCode = authResult.serverAuthCode
            if (authCode != null) {
                coroutineScope.launch {
                    runAuthFlow(
                        authenticator,
                        authCode,
                        onAuth = {
                            error = "Unexpectedly requesting auth flow again"
                            ongoingAuth = false
                        },
                        onSuccess = onSuccess,
                        onError = { e ->
                            error = e.message
                            ongoingAuth = false
                        }
                    )
                }
            } else {
                error = "No auth code in result"
                ongoingAuth = false
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                ongoingAuth = true
                coroutineScope.launch {
                    runAuthFlow(
                        authenticator,
                        null,
                        onAuth = { result ->
                            val pendingIntent = result.pendingIntent
                            if (pendingIntent != null) {
                                startForResult.launch(IntentSenderRequest.Builder(pendingIntent).build())
                            } else {
                                error = "No pending intent in auth result"
                                ongoingAuth = false
                            }
                        },
                        onSuccess = onSuccess,
                        onError = { e ->
                            error = e.message
                            ongoingAuth = false
                        }
                    )
                }
            },
            enabled = !ongoingAuth
        ) {
            Box(modifier, contentAlignment = Alignment.Center) {
                AnimatedContent(ongoingAuth, label = "authorize_button_content") { ongoing ->
                    if (ongoing) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 1.dp)
                    } else {
                        Text(stringResource(Res.string.onboarding_screen_authorize_cta))
                    }
                }
            }
        }

        AnimatedContent(error, label = "authorize_error_message") { message ->
            Text(message ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

suspend fun runAuthFlow(
    authenticator: GoogleAuthenticator,
    providedAuthCode: String?,
    onAuth: (AuthorizationResult) -> Unit,
    onSuccess: (GoogleAuthenticator.OAuthToken) -> Unit,
    onError: (Exception) -> Unit,
) {
    val scope = listOf(
        GoogleAuthenticator.Scope.Profile,
        GoogleAuthenticator.Scope(TasksScopes.Tasks),
    )

    try {
        val authCode = providedAuthCode ?: authenticator.authorize(scope, true) {
            val result = it as AuthorizationResult
            onAuth(result)
        }
        if (authCode.isNotEmpty()) {
            val grant = GoogleAuthenticator.Grant.AuthorizationCode(authCode)
            val oauthToken = authenticator.getToken(grant)
            onSuccess(oauthToken)
        }
    } catch (e: Exception) {
        onError(e)
    }
}