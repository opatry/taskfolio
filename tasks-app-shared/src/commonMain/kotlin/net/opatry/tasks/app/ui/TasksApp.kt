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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import net.opatry.tasks.app.ui.screen.SearchScreen
import net.opatry.tasks.app.ui.screen.SettingsScreen
import net.opatry.tasks.app.ui.screen.TaskListsScreen
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.app_name
import net.opatry.tasks.resources.navigation_search
import net.opatry.tasks.resources.navigation_settings
import net.opatry.tasks.resources.navigation_tasks
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import ListTodo as LucideListTodo
import RefreshCw as LucideRefreshClockWise
import Search as LucideSearch
import Settings as LucideSettings

enum class Destination(
    val labelRes: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource? = null,
) {
    Tasks(Res.string.navigation_tasks, LucideListTodo),
    Search(Res.string.navigation_search, LucideSearch),
    Settings(Res.string.navigation_settings, LucideSettings),
}

@Composable
fun TasksApp(viewModel: TaskListsViewModel) {
    val httpClient = koinInject<HttpClient>(named(HttpClientName.Tasks))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.app_name))
                },
                actions = {
                    IconButton(onClick = viewModel::fetch) {
                        Icon(LucideRefreshClockWise, null) // TODO stringRes("refresh")
                    }
                    ProfileIcon(httpClient)
                }
            )
        },
    ) { innerPadding ->
        // TODO scaffold + bottom bar vs nav rail WindowSizeClass
        // TODO where to put Theme+Surface

        val selectedItem = remember { mutableStateOf(Destination.Tasks) }
        NavigationSuiteScaffold(navigationSuiteItems = {
            Destination.entries.forEach { destination ->
                item(
                    selected = selectedItem.value == destination,
                    onClick = { selectedItem.value = destination },
                    label = { Text(stringResource(destination.labelRes)) },
                    icon = {
                        Icon(destination.icon, destination.contentDescription?.let { stringResource(it) })
                    },
                    alwaysShowLabel = true,
                )
            }
        }) {
            Column {
                when (selectedItem.value) {
                    Destination.Tasks -> TaskListsScreen(viewModel)
                    Destination.Search -> SearchScreen()
                    Destination.Settings -> SettingsScreen()
                }
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