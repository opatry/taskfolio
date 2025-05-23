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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.window.Window
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppMenuBarTest {
    @Test
    fun `when showDevelopmentTools is false then tools menu should be hidden`() = runComposeUiTest {
        setContent {
            Window(
                onCloseRequest = {},
                title = "Test",
            ) {
                AppMenuBar(
                    showDevelopmentTools = false,
                    onNewTaskListClick = {},
                    onNewTaskClick = {},
                    onAboutClick = {},
                    onNetworkLogClick = {},
                )
            }
        }

        onNodeWithText("Tools")
            .assertDoesNotExist()
    }

    @Test
    fun `when clicking on show network logs then should trigger callback`() = runComposeUiTest {
        var isNetworkLogClicked = false
        setContent {
            Window(
                onCloseRequest = {},
                title = "Test",
            ) {
                AppMenuBar(
                    showDevelopmentTools = true,
                    onNewTaskListClick = {},
                    onNewTaskClick = {},
                    onAboutClick = {},
                    onNetworkLogClick = { isNetworkLogClicked = true },
                )
            }
        }

        onNodeWithText("Tools")
            .assertIsDisplayed()
            .performClick()

        onNodeWithText("Show network logs")
            .assertIsDisplayed()
            .performClick()
    }
}