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

package net.opatry.tasks.app.ui.screen

import LucideIcons
import ShieldCheck
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TasksScopes
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.onboarding_screen_authorize_cta
import net.opatry.tasks.resources.onboarding_screen_authorize_explanation
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@Composable
actual fun AuthorizationScreen(onSuccess: (GoogleAuthenticator.OAuthToken) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val authenticator = koinInject<GoogleAuthenticator>()
    var ongoingAuth by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Icon(LucideIcons.ShieldCheck, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(Res.string.onboarding_screen_authorize_explanation), textAlign = TextAlign.Center)

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
                            GoogleAuthenticator.Scope.Profile,
                            GoogleAuthenticator.Scope(TasksScopes.Tasks),
                        )
                        try {
                            val authCode = authenticator.authorize(scope, true) {
                                uriHandler.openUri(it as String)
                            }.let(GoogleAuthenticator.Grant::AuthorizationCode)
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
                Text(stringResource(Res.string.onboarding_screen_authorize_cta))
            }
        }
        AnimatedContent(error, label = "authorize_error_message") {
            Text(it ?: "")
        }
    }
}