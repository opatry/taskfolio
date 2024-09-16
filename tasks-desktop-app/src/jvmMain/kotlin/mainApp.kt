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
import kotlinx.coroutines.launch
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.tasks.TaskListsApi
import net.opatry.google.tasks.model.TaskList
import net.opatry.tasks.app.TasksApp


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()
        var taskLists by remember { mutableStateOf(emptyList<TaskList>()) }

        Column {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val httpClient = buildGoogleHttpClient(
                            "https://tasks.googleapis.com",
                            "client_secret_1018227543555-k121h4da66i87lpione39a7et0lkifqi.apps.googleusercontent.com.json",
                            listOf(GoogleAuthenticator.Permission.Tasks)
                        ,
                        uriHandler::openUri)
                        val api = TaskListsApi(httpClient)
                        taskLists = api.listAll()
                    }
                }
            ) {
                Text("Authenticate")
            }
            TasksApp(taskLists)
        }
    }
}