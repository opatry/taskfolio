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

package net.opatry.tasks.app.ui

import AlignJustify
import Calendar
import ListTodo
import LucideIcons
import RefreshCw
import Search
import Settings
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.opatry.google.profile.model.UserInfo
import net.opatry.tasks.app.di.HttpClientName
import net.opatry.tasks.app.ui.component.MissingScreen
import net.opatry.tasks.app.ui.screen.TaskListsMasterDetail
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.app_name
import net.opatry.tasks.resources.navigation_calendar
import net.opatry.tasks.resources.navigation_search
import net.opatry.tasks.resources.navigation_settings
import net.opatry.tasks.resources.navigation_tasks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

enum class AppTasksScreen(
    val labelRes: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource? = null,
) {
    Tasks(Res.string.navigation_tasks, LucideIcons.ListTodo),
    Calendar(Res.string.navigation_calendar, LucideIcons.Calendar),
    Search(Res.string.navigation_search, LucideIcons.Search),
    Settings(Res.string.navigation_settings, LucideIcons.Settings),
}

@Composable
fun TasksApp(viewModel: TaskListsViewModel) {
    val httpClient = koinInject<HttpClient>(named(HttpClientName.Tasks))

    var selectedScreen by remember { mutableStateOf(AppTasksScreen.Tasks) }

    NavigationSuiteScaffold(navigationSuiteItems = {
        // Only if expanded state
        if (false) {
            item(
                selected = false,
                onClick = { },
                enabled = false,
                icon = {
                    Icon(LucideIcons.AlignJustify, null)
                },
                alwaysShowLabel = false,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        AppTasksScreen.entries.forEach { screen ->
            item(
                selected = selectedScreen == screen,
                onClick = { selectedScreen = screen },
                label = { Text(stringResource(screen.labelRes)) },
                icon = {
                    Icon(screen.icon, screen.contentDescription?.let { stringResource(it) })
                },
                alwaysShowLabel = false,
            )
        }
    }) {
        Column {
            when (selectedScreen) {
                AppTasksScreen.Tasks -> {
                    Card(
                        Modifier.padding(16.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(Res.string.app_name),
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = viewModel::fetch) {
                                Icon(LucideIcons.RefreshCw, null) // TODO stringRes("refresh")
                            }
                            ProfileIcon(httpClient)
                        }
                    }

                    TaskListsMasterDetail(viewModel)
                }

                AppTasksScreen.Calendar -> MissingScreen(stringResource(AppTasksScreen.Calendar.labelRes), LucideIcons.Calendar)
                AppTasksScreen.Search -> MissingScreen(stringResource(AppTasksScreen.Search.labelRes), LucideIcons.Search)
                AppTasksScreen.Settings -> MissingScreen(stringResource(AppTasksScreen.Settings.labelRes), LucideIcons.Settings)
            }
        }
    }
}

@Composable
fun ProfileIcon(httpClient: HttpClient?) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserInfo?>(null) }
//    val avatarUrl = profile?.photos?.firstOrNull {
//        it.isDefault || it.metadata?.isPrimary == true
//    }?.url
    val avatarUrl by remember {
        derivedStateOf {
            profile?.picture
        }
    }

    if (httpClient != null) {
        LaunchedEffect(Unit) {
            // TODO use dedicated http client without hardcoded URL host
//            val personFields =
//                listOf(FieldMask.Names, FieldMask.EmailAddresses, FieldMask.Photos).joinToString(",") { it.toString() }
//            val queryParams = mapOf(
//                "personFields" to personFields
//            ).entries.joinToString(prefix = "?", separator = "&") {
//                "${it.key}=${it.value}"
//            }
//            val response = httpClient.get("https://people.googleapis.com/v1/people/me${queryParams}")
            val response = httpClient.get("https://www.googleapis.com/oauth2/v1/userinfo?alt=json")

            if (response.status.isSuccess()) {
                profile = response.body()
            } else {
                // TODO snackbar or error icon
                println(response.bodyAsText())
//                throw ClientRequestException(response, response.bodyAsText())
            }
        }
    }

    IconButton(onClick = { }, enabled = false) {
        // TODO Depending on UserState type, display progress or error or avatar or fallback

        Crossfade(targetState = avatarUrl != null, label = "avatar_crossfade") { hasAvatar ->
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (hasAvatar) {
                    AsyncImage(avatarUrl, null, Modifier.clip(CircleShape))
                } else {
                    CircularProgressIndicator(strokeWidth = 1.dp, color = LocalContentColor.current)
                }
            }
        }
    }
}