/*
 * Lucide License
 * ISC License
 *
 * Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part of Feather (MIT).
 * All other copyright (c) for Lucide are held by Lucide Contributors 2022.
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without
 * fee is hereby granted, provided that the above copyright notice and this permission notice
 * appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE
 * USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val LucideIcons.Github: ImageVector
    get() {
        if (_Github != null) {
            return _Github!!
        }
        _Github = ImageVector.Builder(
            name = "Github",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(15f, 22f)
                verticalLineToRelative(-4f)
                arcToRelative(4.8f, 4.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, -3.5f)
                curveToRelative(3f, 0f, 6f, -2f, 6f, -5.5f)
                curveToRelative(0.08f, -1.25f, -0.27f, -2.48f, -1f, -3.5f)
                curveToRelative(0.28f, -1.15f, 0.28f, -2.35f, 0f, -3.5f)
                curveToRelative(0f, 0f, -1f, 0f, -3f, 1.5f)
                curveToRelative(-2.64f, -0.5f, -5.36f, -0.5f, -8f, 0f)
                curveTo(6f, 2f, 5f, 2f, 5f, 2f)
                curveToRelative(-0.3f, 1.15f, -0.3f, 2.35f, 0f, 3.5f)
                arcTo(5.403f, 5.403f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 9f)
                curveToRelative(0f, 3.5f, 3f, 5.5f, 6f, 5.5f)
                curveToRelative(-0.39f, 0.49f, -0.68f, 1.05f, -0.85f, 1.65f)
                curveToRelative(-0.17f, 0.6f, -0.22f, 1.23f, -0.15f, 1.85f)
                verticalLineToRelative(4f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(Color(0xFF000000)),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(9f, 18f)
                curveToRelative(-4.51f, 2f, -5f, -2f, -7f, -2f)
            }
        }.build()
        return _Github!!
    }

private var _Github: ImageVector? = null
