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

package net.opatry.tasks.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.presentation.UserState
import net.opatry.tasks.app.ui.component.ProfileIcon
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
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileIconTest {

    @Test
    fun `when missing user state then LOADING_INDICATOR should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = null,
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(LOADING_INDICATOR)
            .assertIsDisplayed()
    }

    @Test
    fun `when unsigned user state then UNSIGNED_ICON should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.Unsigned,
                showUserMenu = false,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(UNSIGNED_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when newcomer user state then UNSIGNED_ICON should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.Newcomer,
                showUserMenu = false,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(UNSIGNED_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Ignore // AsyncImage seems to cause trouble in tests, need investigation
    @Test
    fun `when signed user state with avatar then AVATAR_IMAGE should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto", avatarUrl = "TBD"),
                showUserMenu = false,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(AVATAR_IMAGE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when signed user state without avatar then FALLBACK_AVATAR_ICON should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto", avatarUrl = null),
                showUserMenu = false,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(FALLBACK_AVATAR_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when menu is expanded and user state is signed then user info should be displayed`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto", email = "plop@acme.org"),
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(USER_NAME)
            .assertIsDisplayed()
            .assertTextEquals("toto")

        onNodeWithTag(USER_EMAIL)
            .assertIsDisplayed()
            .assertTextEquals("plop@acme.org")

        onNodeWithTag(SIGN_OUT_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when menu is expanded and user state has no email then email placeholder should be displayed`() = runComposeUiTest {
        lateinit var emailPlaceholderStr: String
        setContent {
            emailPlaceholderStr = stringResource(Res.string.profile_popup_no_email)
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto", email = null),
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(USER_EMAIL)
            .assertIsDisplayed()
            .assertTextEquals(emailPlaceholderStr)
    }

    @Test
    fun `when menu is expanded and user state is signed in and sign out button is clicked then onSignOut is triggered`() = runComposeUiTest {
        var signOutTriggered = false
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto"),
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {
                    signOutTriggered = true
                },
                authorizeButton = {},
            )
        }

        onNodeWithTag(SIGN_OUT_BUTTON)
            .performClick()

        assertTrue(signOutTriggered)
    }

    @Test
    fun `when menu is expanded and user state is unsigned then authorize button is displayed`() = runComposeUiTest {
        var authorizeButtonConsumed = false
        setContent {
            ProfileIcon(
                userState = UserState.Unsigned,
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {
                    authorizeButtonConsumed = true
                },
            )
        }

        onNodeWithTag(SIGN_IN_EXPLANATION)
            .assertIsDisplayed()

        assertTrue(authorizeButtonConsumed)
    }

    @Test
    fun `when menu is collapsed and icon is clicked then onExpand should be triggered`() = runComposeUiTest {
        var expandTriggered = false
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto"),
                showUserMenu = false,
                onExpand = {
                    expandTriggered = true
                },
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(PROFILE_MENU_TOGGLE)
            .assertIsEnabled()
            .performClick()

        assertTrue(expandTriggered)
    }

    @Test
    fun `when menu is expanded then icon should be disabled`() = runComposeUiTest {
        setContent {
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto"),
                showUserMenu = true,
                onExpand = {},
                onCollapse = {},
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithTag(PROFILE_MENU_TOGGLE)
            .assertIsNotEnabled()
    }

    @Test
    fun `when menu is expanded and tap outside then onCollapse should be triggered`() = runComposeUiTest {
        var collapseTriggered = false
        setContent {
            Row {
                Text("Outside")
            }
            ProfileIcon(
                userState = UserState.SignedIn(name = "toto"),
                showUserMenu = true,
                onExpand = {},
                onCollapse = {
                    collapseTriggered = true
                },
                onSignOut = {},
                authorizeButton = {},
            )
        }

        onNodeWithText("Outside")
            .performClick()

        assertTrue(collapseTriggered)
    }
}