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

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TasksScopes
import net.opatry.tasks.app.di.tasksModule
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.onboarding_screen_authorize_cta
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(tasksModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication) {
            val viewModel = koinViewModel<TaskListsViewModel>()

            val uriHandler = LocalUriHandler.current
            val coroutineScope = rememberCoroutineScope()

            var httpClient by remember { mutableStateOf<HttpClient?>(null) }

            Column {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            httpClient = buildGoogleHttpClient(
                                "https://tasks.googleapis.com",
                                "client_secret_1018227543555-k121h4da66i87lpione39a7et0lkifqi.apps.googleusercontent.com.json",
                                listOf(
                                    GoogleAuthenticator.Permission.Profile,
                                    GoogleAuthenticator.Permission(TasksScopes.Tasks),
                                ),
                                uriHandler::openUri
                            )
                        }
                    }
                ) {
                    Text(stringResource(Res.string.onboarding_screen_authorize_cta))
                }
                TasksApp(viewModel)
            }
        }
    }
}