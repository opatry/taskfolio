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

package net.opatry.tasks.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TasksScopes
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.app.ui.theme.TasksAppTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

enum class SignInStatus {
    Loading,
    SignedIn,
    SignedOut,
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var signInStatus by remember { mutableStateOf(SignInStatus.Loading) }
            val credentialsStorage = koinInject<CredentialsStorage>()
            LaunchedEffect(Unit) {
                signInStatus = if (credentialsStorage.load() != null) {
                    SignInStatus.SignedIn
                } else {
                    SignInStatus.SignedOut
                }
            }

            TasksAppTheme {
                Surface {
                    when (signInStatus) {
                        SignInStatus.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 1.dp)
                            }
                        }

                        SignInStatus.SignedIn -> {
                            val viewModel = koinViewModel<TaskListsViewModel>()
                            TasksApp(viewModel)
                        }

                        SignInStatus.SignedOut -> {
                            val t0 = Clock.System.now()
                            AuthorizationScreen { token ->
                                signInStatus = SignInStatus.SignedIn
                                coroutineScope.launch {
                                    credentialsStorage.store(
                                        TokenCache(
                                            token.accessToken,
                                            token.refreshToken,
                                            (t0 + token.expiresIn.seconds).toEpochMilliseconds()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorizationScreen(onSuccess: (GoogleAuthenticator.OAuthToken) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val authenticator = koinInject<GoogleAuthenticator>()
    var ongoingAuth by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ongoingAuth) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 1.dp)
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Button(
                onClick = {
                    ongoingAuth = true
                    coroutineScope.launch {
                        val scope = listOf(
                            GoogleAuthenticator.Permission.Profile,
                            GoogleAuthenticator.Permission(TasksScopes.Tasks),
                        )
                        try {
                            val authCode = authenticator.authorize(scope, true, uriHandler::openUri)
                                .let(GoogleAuthenticator.Grant::AuthorizationCode)
                            val oauthToken = authenticator.getToken(authCode)
                            onSuccess(oauthToken)
                        } catch (e: Exception) {
                            error = e.message
                            ongoingAuth = false
                        }
                    }
                },
                enabled = !ongoingAuth
            ) {
                // FIXME Res from shared/library module
//                Text(stringResource(Res.string.onboarding_screen_authorize_cta))
                Text("Authorize")
            }
        }
        AnimatedContent(error, label = "authorize_error_message") {
            Text(it ?: "")
        }
    }
}