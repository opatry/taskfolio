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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.app.ui.UserState
import net.opatry.tasks.app.ui.UserViewModel
import net.opatry.tasks.app.ui.screen.AuthorizationScreen
import net.opatry.tasks.app.ui.theme.TasksfolioTheme
import org.koin.compose.viewmodel.koinViewModel


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userViewModel = koinViewModel<UserViewModel>()
            val userState by userViewModel.state.collectAsState(null)

            if (userState == null) {
                LaunchedEffect(userState) {
                    userViewModel.refreshUserState()
                }
            }

            TasksfolioTheme {
                Surface {
                    when (userState) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 1.dp)
                        }

                        is UserState.Unsigned,
                        is UserState.SignedIn -> {
                            val tasksViewModel = koinViewModel<TaskListsViewModel>()
                            TasksApp(userViewModel, tasksViewModel)
                        }

                        is UserState.Newcomer -> AuthorizationScreen(
                            onSkip = userViewModel::skipSignIn,
                            onSuccess = userViewModel::signIn,
                        )
                    }
                }
            }
        }
    }
}
