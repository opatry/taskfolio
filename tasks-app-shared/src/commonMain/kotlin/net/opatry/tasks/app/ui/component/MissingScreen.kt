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

import AppWindow
import LucideIcons
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MissingScreen(screenName: String, screenIcon: ImageVector? = null) {
    EmptyState(
        icon = screenIcon ?: LucideIcons.AppWindow,
        title = "Missing screen '$screenName'",
        description = "This screen is not implemented yet",
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview
@Composable
private fun MissingScreenPreview() {
    TaskfolioThemedPreview {
        MissingScreen("Some feature", LucideIcons.AppWindow)
    }
}
