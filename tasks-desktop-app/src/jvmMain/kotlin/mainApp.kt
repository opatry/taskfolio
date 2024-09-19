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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TasksScopes
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import net.opatry.tasks.app.di.tasksModule
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.onboarding_screen_authorize_cta
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import kotlin.time.Duration.Companion.seconds

enum class SignInStatus {
    Loading,
    SignedIn,
    SignedOut,
}

fun main() {
    startKoin {
        modules(tasksModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication) {
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

            when (signInStatus) {
                SignInStatus.Loading -> CircularProgressIndicator()
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

@Composable
fun AuthorizationScreen(onSuccess: (GoogleAuthenticator.OAuthToken) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val authenticator = koinInject<GoogleAuthenticator>()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = {
                coroutineScope.launch {
                    val scope = listOf(
                        GoogleAuthenticator.Permission.Profile,
                        GoogleAuthenticator.Permission(TasksScopes.Tasks),
                    )
                    val authCode = authenticator.authorize(scope, true, uriHandler::openUri).let(GoogleAuthenticator.Grant::AuthorizationCode)
                    val oauthToken = authenticator.getToken(authCode)
                    onSuccess(oauthToken)
                }
            }
        ) {
            Text(stringResource(Res.string.onboarding_screen_authorize_cta))
        }
    }
}