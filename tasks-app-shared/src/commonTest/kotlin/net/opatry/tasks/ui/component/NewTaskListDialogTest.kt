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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.CANCEL_BUTTON
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.TEXT_FIELD
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.VALIDATE_BUTTON
import net.opatry.tasks.app.ui.component.NewTaskListDialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NewTaskListDialogTest {
    @Test
    fun `when clicking create then should trigger callback with title`() = runComposeUiTest {
        var taskTitle: String? = null
        setContent {
            NewTaskListDialog(
                onDismissRequest = {},
                onCreate = { taskTitle = it },
            )
        }

        onNodeWithTag(TEXT_FIELD)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsFocused()
            .performTextInput("test title")

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals("test title", taskTitle)
    }

    @Test
    fun `when title is blank then create button should be disabled`() = runComposeUiTest {
        setContent {
            NewTaskListDialog(
                onDismissRequest = {},
                onCreate = {},
            )
        }

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when dismissed then should trigger dismiss callback`() = runComposeUiTest {
        var isDismissed = false
        setContent {
            Column {
                Text("Outside", Modifier.clickable {
                    println("HERE I AM")
                })
                NewTaskListDialog(
                    onDismissRequest = { isDismissed = true },
                    onCreate = {},
                )
            }
        }

        onNodeWithText("Outside")
            .performClick()

        assertTrue(isDismissed)
    }

    @Test
    fun `when canceled then should trigger dismiss callback`() = runComposeUiTest {
        var isDismissed = false
        setContent {
            NewTaskListDialog(
                onDismissRequest = { isDismissed = true },
                onCreate = {},
            )
        }

        onNodeWithTag(CANCEL_BUTTON)
            .performClick()

        assertTrue(isDismissed)
    }

    @Test
    fun `when default title is provided then text field should contain it and create button be enabled`() = runComposeUiTest {
        setContent {
            NewTaskListDialog(
                defaultTitle = "default title",
                onDismissRequest = {},
                onCreate = {},
            )
        }

        onNodeWithTag(TEXT_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("default title")

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}