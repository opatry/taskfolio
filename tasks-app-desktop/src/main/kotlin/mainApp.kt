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

import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import net.opatry.tasks.app.di.authModule
import net.opatry.tasks.app.di.dataModule
import net.opatry.tasks.app.di.networkModule
import net.opatry.tasks.app.di.platformModule
import net.opatry.tasks.app.di.tasksAppModule
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.app.ui.UserState
import net.opatry.tasks.app.ui.UserViewModel
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.screen.AboutApp
import net.opatry.tasks.app.ui.screen.AuthorizationScreen
import net.opatry.tasks.app.ui.theme.TaskfolioTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.UIManager

private const val GCP_CLIENT_ID = "191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com"

object MainApp

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    val appName = System.getProperty("app.name") ?: "Taskfolio"
    val fullVersion = System.getProperty("app.version.full") ?: "0.0.0.0"
    val versionLabel = System.getProperty("app.version")?.let { " v$it" } ?: ""
    application {
        val screenSize by remember {
            mutableStateOf(Toolkit.getDefaultToolkit().screenSize)
        }

        val defaultSize = DpSize(1024.dp, 800.dp)
        val minSize = Dimension(600, 400)

        var windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center), width = defaultSize.width, height = defaultSize.height
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "$appName$versionLabel",
        ) {
            if (window.minimumSize != minSize) window.minimumSize = minSize

            if (windowState.placement == WindowPlacement.Floating) {
                val insets = window.insets
                val maxWidth = screenSize.width - insets.left - insets.right
                val finalWidth = if (window.size.width > maxWidth) maxWidth else window.size.width
                val maxHeight = screenSize.height - insets.bottom - insets.top
                val finalHeight = if (window.size.height > maxHeight) maxHeight else window.size.height

                if (finalWidth != window.size.width || finalHeight != window.size.height) {
                    windowState = WindowState(
                        placement = windowState.placement,
                        position = windowState.position,
                        isMinimized = windowState.isMinimized,
                        width = finalWidth.dp,
                        height = finalHeight.dp
                    )
                }
            }

            KoinApplication(application = {
                modules(
                    platformModule(),
                    dataModule,
                    authModule(GCP_CLIENT_ID),
                    networkModule,
                    tasksAppModule,
                )
            }) {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.state.collectAsState(null)

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
                                    MainApp::class.java.getResource("/licenses_desktop.json")?.readText() ?: ""
                                }
                                val tasksViewModel = koinViewModel<TaskListsViewModel>()
                                TasksApp(aboutApp, userViewModel, tasksViewModel)
                            }

                            UserState.Newcomer -> AuthorizationScreen(
                                onSkip = userViewModel::skipSignIn,
                                onSuccess = userViewModel::signIn,
                            )
                        }
                    }
                }
            }
        }
    }
}
