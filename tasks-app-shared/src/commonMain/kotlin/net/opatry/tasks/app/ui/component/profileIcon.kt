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
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import net.opatry.tasks.app.presentation.UserState
import net.opatry.tasks.app.presentation.UserViewModel
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.AVATAR_IMAGE
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.FALLBACK_AVATAR_ICON
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.LOADING_INDICATOR
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.PROFILE_MENU_TOGGLE
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.SIGN_IN_EXPLANATION
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.SIGN_OUT_BUTTON
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.UNSIGNED_ICON
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.USER_EMAIL
import net.opatry.tasks.app.ui.component.ProfileIconTestTag.USER_NAME
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.profile_popup_no_email
import net.opatry.tasks.resources.profile_popup_sign_explanation
import net.opatry.tasks.resources.profile_popup_sign_out
import org.jetbrains.compose.resources.stringResource

@VisibleForTesting
internal object ProfileIconTestTag {
    const val PROFILE_MENU_TOGGLE = "PROFILE_ICON_MENU_TOGGLE"
    const val LOADING_INDICATOR = "PROFILE_ICON_LOADING_INDICATOR"
    const val UNSIGNED_ICON = "PROFILE_ICON_UNSIGNED"
    const val AVATAR_IMAGE = "PROFILE_ICON_AVATAR_IMAGE"
    const val FALLBACK_AVATAR_ICON = "PROFILE_ICON_FALLBACK_ICON"
    const val USER_NAME = "PROFILE_ICON_USER_NAME"
    const val USER_EMAIL = "PROFILE_ICON_USER_EMAIL"
    const val SIGN_OUT_BUTTON = "PROFILE_ICON_SIGN_OUT_BUTTON"
    const val SIGN_IN_EXPLANATION = "PROFILE_ICON_SIGN_IN_EXPLANATION"
}

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
        onSignOut = {
            viewModel.signOut()
            showUserMenu = false
        },
    ) {
        AuthorizeGoogleTasksButton(
            Modifier.align(Alignment.CenterHorizontally),
            onSuccess = {
                viewModel.signIn(it)
                showUserMenu = false
            },
        )
    }
}

@Composable
fun ProfileIcon(
    userState: UserState?,
    showUserMenu: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onSignOut: () -> Unit = {},
    authorizeButton: @Composable ColumnScope.() -> Unit,
) {

    IconButton(
        onClick = onExpand,
        modifier = Modifier.testTag(PROFILE_MENU_TOGGLE),
        enabled = !showUserMenu && userState != null,
    ) {
        Crossfade(targetState = userState, label = "avatar_crossfade") { state ->
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    null -> LoadingIndicator(
                        modifier = Modifier.testTag(LOADING_INDICATOR),
                        color = LocalContentColor.current
                    )
                    is UserState.Newcomer,
                    is UserState.Unsigned -> Icon(
                        imageVector = LucideIcons.CircleUserRound,
                        contentDescription = null,
                        modifier = Modifier.testTag(UNSIGNED_ICON),
                    )

                    is UserState.SignedIn -> {
                        if (state.avatarUrl != null) {
                            AsyncImage(
                                model = state.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag(AVATAR_IMAGE)
                            )
                        } else {
                            Icon(
                                imageVector = LucideIcons.CircleUserRound,
                                contentDescription = null,
                                modifier = Modifier.testTag(FALLBACK_AVATAR_ICON),
                            )
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
                                        Text(
                                            text = state.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.testTag(USER_NAME),
                                        )
                                        Text(
                                            text = state.email ?: stringResource(Res.string.profile_popup_no_email),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag(USER_EMAIL),
                                        )

                                        Spacer(Modifier.size(8.dp))
                                        Button(
                                            onClick = onSignOut,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .testTag(SIGN_OUT_BUTTON),
                                        ) {
                                            // TODO confirmation dialog?
                                            Text(stringResource(Res.string.profile_popup_sign_out))
                                        }
                                    }

                                    UserState.Unsigned -> {
                                        Text(
                                            text = stringResource(Res.string.profile_popup_sign_explanation),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.testTag(SIGN_IN_EXPLANATION),
                                        )

                                        Spacer(Modifier.size(8.dp))
                                        authorizeButton()
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