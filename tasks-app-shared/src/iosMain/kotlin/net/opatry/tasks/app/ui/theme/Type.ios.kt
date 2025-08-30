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

package net.opatry.tasks.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

internal actual val Typography: androidx.compose.material3.Typography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontSize = 57.sp), // TODO adjust
    displayMedium = TextStyle(fontSize = 45.sp), // TODO adjust
    displaySmall = TextStyle(fontSize = 36.sp), // TODO adjust
    headlineLarge = TextStyle(fontSize = 32.sp), // TODO adjust
    headlineMedium = TextStyle(fontSize = 28.sp), // TODO adjust
    headlineSmall = TextStyle(fontSize = 24.sp), // TODO adjust
    titleLarge = TextStyle(fontSize = 22.sp), // TODO adjust
    titleMedium = TextStyle(fontSize = 16.sp), // TODO adjust
    titleSmall = TextStyle(fontSize = 14.sp), // TODO adjust
    bodyLarge = TextStyle(fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 12.sp),
    bodySmall = TextStyle(fontSize = 10.sp),
    labelLarge = TextStyle(fontSize = 12.sp),
    labelMedium = TextStyle(fontSize = 10.sp),
    labelSmall = TextStyle(fontSize = 9.sp),
)
