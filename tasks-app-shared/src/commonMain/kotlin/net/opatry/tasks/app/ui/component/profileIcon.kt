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

package net.opatry.tasks.app.ui.component

import CircleUserRound
import LucideIcons
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.opatry.google.profile.model.UserInfo

// TODO would be nice to rely on a local cache for the profile info
//  - profile info
//  - avatar image bitmap on disk (can Coil handle that?)
sealed class ProfileState {
    data object Loading : ProfileState()
    data class Error(val message: String) : ProfileState()
    data class Success(val profile: UserInfo) : ProfileState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileIcon(httpClient: HttpClient?) {
    var profileState by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }

    if (httpClient != null) {
        LaunchedEffect(Unit) {
            try {
                val response = httpClient.get("https://www.googleapis.com/oauth2/v1/userinfo?alt=json")

                profileState = if (response.status.isSuccess()) {
                    ProfileState.Success(response.body())
                } else {
                    ProfileState.Error(response.bodyAsText())
                }
            } catch (e: Exception) {
                // TODO find a way to retry after a delay (on focus?)
                // most likely no network
                profileState = ProfileState.Error(e.message ?: "Unknown error (${e.javaClass.simpleName})")
            }
        }
    }

    Crossfade(targetState = profileState, label = "avatar_crossfade") { state ->
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (state) {
                is ProfileState.Loading -> CircularProgressIndicator(strokeWidth = 1.dp, color = LocalContentColor.current)
                is ProfileState.Error -> {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                        tooltip = {
                            RichTooltip(title = { Text("Profile fetch error") }) {
                                Text(state.message, fontFamily = FontFamily.Monospace)
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        BadgedBox(badge = { Badge { Text("!") } }) {
                            Icon(LucideIcons.CircleUserRound, null)
                        }
                    }
                }

                is ProfileState.Success -> {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                        tooltip = {
                            RichTooltip(title = { Text(state.profile.name) }) {
                                Text(state.profile.email ?: "No email information", fontFamily = FontFamily.Monospace)
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        val avatarUrl = state.profile.picture
                        if (avatarUrl != null) {
                            AsyncImage(
                                state.profile.picture,
                                null,
                                Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else {
                            Icon(LucideIcons.CircleUserRound, null)
                        }
                    }
                }
            }
        }
    }
}