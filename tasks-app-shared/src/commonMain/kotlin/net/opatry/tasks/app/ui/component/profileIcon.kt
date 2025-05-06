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

package net.opatry.tasks.app.ui.component

import CircleUserRound
import LucideIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.tasks.app.ui.UserState
import net.opatry.tasks.app.ui.UserViewModel
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.profile_popup_no_email
import net.opatry.tasks.resources.profile_popup_sign_explanation
import net.opatry.tasks.resources.profile_popup_sign_out
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileIcon(viewModel: UserViewModel) {
    val userState by viewModel.state.collectAsStateWithLifecycle(null)
    var showUserMenu by remember { mutableStateOf(false) }

    ProfileIcon(
        userState = userState,
        showUserMenu = showUserMenu,
        onExpand = {
            showUserMenu = true
        },
        onCollapse = {
            showUserMenu = false
        },
        onSignIn = {
            viewModel.signIn(it)
            showUserMenu = false
        },
        onSignOut = {
            viewModel.signOut()
            showUserMenu = false
        },
    )
}

@Composable
fun ProfileIcon(
    userState: UserState?,
    showUserMenu: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onSignIn: (GoogleAuthenticator.OAuthToken) -> Unit = {},
    onSignOut: () -> Unit = {},
) {

    IconButton(
        onClick = onExpand,
        enabled = !showUserMenu && userState != null
    ) {
        Crossfade(targetState = userState, label = "avatar_crossfade") { state ->
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    null -> LoadingIndicator(color = LocalContentColor.current)
                    is UserState.Newcomer,
                    is UserState.Unsigned -> Icon(LucideIcons.CircleUserRound, null)

                    is UserState.SignedIn -> {
                        if (state.avatarUrl != null) {
                            AsyncImage(
                                state.avatarUrl,
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

                AnimatedVisibility(showUserMenu) {
                    // FIXME sticks to window right edge, how to offset it on the left?
                    //  negative offset doesn't work, alignment doesn't help either
                    Popup(onDismissRequest = onCollapse) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                Modifier
                                    .widthIn(200.dp, 300.dp)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                when (state) {
                                    is UserState.SignedIn -> {
                                        Text(state.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            state.email ?: stringResource(Res.string.profile_popup_no_email),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Spacer(Modifier.size(8.dp))
                                        Button(
                                            onClick = onSignOut,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            // TODO confirmation dialog?
                                            Text(stringResource(Res.string.profile_popup_sign_out))
                                        }
                                    }

                                    UserState.Unsigned -> {
                                        Text(
                                            stringResource(Res.string.profile_popup_sign_explanation),
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(Modifier.size(8.dp))
                                        AuthorizeGoogleTasksButton(
                                            Modifier.align(Alignment.CenterHorizontally),
                                            onSuccess = onSignIn,
                                        )
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}