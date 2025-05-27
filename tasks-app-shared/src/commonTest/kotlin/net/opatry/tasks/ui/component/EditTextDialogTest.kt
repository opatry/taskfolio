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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import net.opatry.tasks.app.ui.component.EditTextDialog
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.CANCEL_BUTTON
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.DIALOG_TITLE
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.ERROR_MESSAGE
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.TEXT_FIELD
import net.opatry.tasks.app.ui.component.EditTextDialogTestTag.VALIDATE_BUTTON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EditTextDialogTest {
    @Test
    fun `when providing validate label then validate button should use this label`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                onDismissRequest = {},
                validateLabel = "Plop",
                onValidate = {},
            )
        }

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertTextEquals("Plop")
    }

    @Test
    fun `when clicking validate with text then should trigger callback with text`() = runComposeUiTest {
        var text: String? = null
        setContent {
            EditTextDialog(
                onDismissRequest = {},
                dialogTitle = "my title",
                validateLabel = "Validate",
                onValidate = { text = it },
            )
        }

        onNodeWithTag(TEXT_FIELD)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertIsFocused()
            .performTextInput("test value")

        onNodeWithTag(VALIDATE_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals("test value", text)
    }

    @Test
    fun `when input is blank and blank is not allowed then validate button should be disabled`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                onDismissRequest = {},
                validateLabel = "Validate",
                onValidate = {},
                allowBlank = false,
            )
        }

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when input is blank and blank allowed then validate button should be enabled`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                onDismissRequest = {},
                validateLabel = "Validate",
                onValidate = {},
                allowBlank = true,
            )
        }

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when dialog title is provided then title node should be displayed with provided title`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                dialogTitle = "toto",
                validateLabel = "Validate",
                onDismissRequest = {},
                onValidate = {},
            )
        }

        onNodeWithTag(DIALOG_TITLE)
            .assertIsDisplayed()
            .assertTextEquals("toto")
    }

    @Test
    fun `when dialog title is null then no title node should be displayed`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                dialogTitle = null,
                validateLabel = "Validate",
                onDismissRequest = {},
                onValidate = {},
                allowBlank = true,
            )
        }

        onNodeWithTag(DIALOG_TITLE)
            .assertDoesNotExist()
    }

    @Test
    fun `when dismissed then should trigger dismiss callback`() = runComposeUiTest {
        var isDismissed = false
        setContent {
            Column {
                Text("Outside", Modifier.clickable {
                    println("HERE I AM")
                })
                EditTextDialog(
                    onDismissRequest = { isDismissed = true },
                    validateLabel = "Validate",
                    onValidate = {},
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
            EditTextDialog(
                onDismissRequest = { isDismissed = true },
                validateLabel = "Validate",
                onValidate = {},
            )
        }

        onNodeWithTag(CANCEL_BUTTON)
            .performClick()

        assertTrue(isDismissed)
    }

    @Test
    fun `when initial text is provided then text field should contain it and validate button be enabled`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                initialText = "initial text",
                onDismissRequest = {},
                validateLabel = "Validate",
                onValidate = {},
            )
        }

        onNodeWithTag(TEXT_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertTextEquals("initial text")

        onNodeWithTag(VALIDATE_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()

        onNodeWithTag(ERROR_MESSAGE)
            .assertDoesNotExist()
    }

    @Test
    fun `when text is considered invalid then supporting text should be displayed`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                initialText = "initial text",
                onDismissRequest = {},
                validateLabel = "Validate",
                onValidate = {},
                allowBlank = false,
            )
        }

        onNodeWithTag(TEXT_FIELD)
            .performTextClearance()

        onNodeWithTag(ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when initial text is blank and blank is not allowed then supporting text should be hidden`() = runComposeUiTest {
        setContent {
            EditTextDialog(
                initialText = "",
                onDismissRequest = {},
                validateLabel = "Validate",
                onValidate = {},
            )
        }

        onNodeWithTag(ERROR_MESSAGE)
            .assertDoesNotExist()
    }
}