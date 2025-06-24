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

package net.opatry.tasks.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.cinterop.ExperimentalForeignApi
import net.opatry.tasks.app.presentation.TaskListsViewModel
import net.opatry.tasks.app.presentation.UserState
import net.opatry.tasks.app.presentation.UserViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.app.ui.component.AuthorizeGoogleTasksButton
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.screen.AboutApp
import net.opatry.tasks.app.ui.screen.AuthorizationScreen
import net.opatry.tasks.app.ui.theme.TaskfolioTheme
import org.koin.compose.viewmodel.koinViewModel
import platform.Foundation.stringWithContentsOfFile

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalComposeUiApi::class,
)
@Suppress(
    "unused",
    "FunctionName",
)
fun MainViewController() = ComposeUIViewController(
    configure = {
        parallelRendering = true
    }
) {
    val mainBundle = platform.Foundation.NSBundle.mainBundle
    val appName = mainBundle.objectForInfoDictionaryKey("CFBundleDisplayName")?.toString()
        ?: mainBundle.objectForInfoDictionaryKey("CFBundleName")?.toString()
        ?: "Taskfolio"

    val shortVersion = mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
        ?.toString()
        ?.takeUnless(String::isEmpty)
    val versionCode = mainBundle.objectForInfoDictionaryKey("CFBundleVersion")
        ?.toString()
        ?.takeUnless(String::isEmpty)
    val fullVersion = listOfNotNull(
        shortVersion,
        versionCode,
    ).joinToString(separator = ".")
        .ifEmpty { "0.0.0.0" }

    val userViewModel = koinViewModel<UserViewModel>()
    val userState by userViewModel.state.collectAsStateWithLifecycle(null)

    if (userState == null) {
        LaunchedEffect(userState) {
            userViewModel.refreshUserState()
        }
    }

    TaskfolioTheme {
        Surface {
            when (userState) {
                null -> LoadingPane()

                UserState.Unsigned,
                is UserState.SignedIn -> {
                    val aboutApp = AboutApp(
                        name = appName,
                        version = fullVersion
                    ) {
                        val path = mainBundle.pathForResource("licenses_ios", "json")
                            ?: error("licenses_ios.json not found in bundle")

                        platform.Foundation.NSString.stringWithContentsOfFile(
                            path,
                            encoding = platform.Foundation.NSUTF8StringEncoding,
                            error = null
                        ) ?: error("Failed to load licenses_ios.json from bundle path: $path")
                    }
                    val tasksViewModel = koinViewModel<TaskListsViewModel>()
                    TasksApp(aboutApp, userViewModel, tasksViewModel)
                }

                UserState.Newcomer -> AuthorizationScreen(userViewModel::skipSignIn) {
                    AuthorizeGoogleTasksButton(onSuccess = userViewModel::signIn)
                }
            }
        }
    }
}
