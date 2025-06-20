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

import LucideIcons
import WifiOff
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.Banner
import net.opatry.tasks.app.ui.component.BannerTestTag.CLOSE_ICON
import net.opatry.tasks.app.ui.component.BannerTestTag.ICON
import net.opatry.tasks.app.ui.component.BannerTestTag.MESSAGE
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BannerTest {

    @Test
    fun `when displaying banner with message and icon then both message and icon should be displayed`() = runComposeUiTest {
        setContent {
            Banner(message = "titi", icon = LucideIcons.WifiOff) {}
        }

        onNodeWithTag(MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("titi")

        onNodeWithTag(ICON, useUnmergedTree = true)
            .assertIsDisplayed()

        onNodeWithTag(CLOSE_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when displaying banner without icon then both icon should be hidden`() = runComposeUiTest {
        setContent {
            Banner(message = "titi", icon = null) {}
        }

        onNodeWithTag(MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("titi")

        onNodeWithTag(ICON, useUnmergedTree = true)
            .assertDoesNotExist()

        onNodeWithTag(CLOSE_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when clicking on close icon then should trigger onClose callback`() = runComposeUiTest {
        var onCloseClicked = false
        setContent {
            Banner("titi") {
                onCloseClicked = true
            }
        }

        onNodeWithTag(CLOSE_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertTrue(onCloseClicked)
    }
}