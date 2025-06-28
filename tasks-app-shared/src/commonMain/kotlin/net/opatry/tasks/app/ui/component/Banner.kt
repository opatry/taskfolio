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

import CloudOff
import LucideIcons
import WifiOff
import X
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.component.BannerTestTag.BANNER
import net.opatry.tasks.app.ui.component.BannerTestTag.CLOSE_ICON
import net.opatry.tasks.app.ui.component.BannerTestTag.ICON
import net.opatry.tasks.app.ui.component.BannerTestTag.MESSAGE
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

@VisibleForTesting
object BannerTestTag {
    const val BANNER = "BANNER_LAYOUT"
    const val ICON = "BANNER_ICON"
    const val MESSAGE = "BANNER_MESSAGE"
    const val CLOSE_ICON = "BANNER_CLOSE_ICON"
}

@Composable
private fun BannerIcon(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size((16.dp)),
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

@Composable
fun Banner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClose: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .shadow(40.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(8.dp)
            .testTag(BANNER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            BannerIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.testTag(ICON),
            )
        }
        Text(
            text = message,
            modifier = Modifier.weight(1f).testTag(MESSAGE),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
            BannerIcon(
                icon = LucideIcons.X,
                contentDescription = null,
                modifier = Modifier.testTag(CLOSE_ICON),
            )
        }
    }
}

private data class BannerParam(
    val message: String,
    val icon: ImageVector? = null,
)

private class MessagePreviewParameterProvider : PreviewParameterProvider<BannerParam> {
    override val values: Sequence<BannerParam>
        get() = sequenceOf(
            BannerParam(
                message = "This is a simple message on 1 line."
            ),
            BannerParam(
                message = "This is a message that span on several lines\nwith a lot of content to check hot\nit could behave",
                icon = LucideIcons.CloudOff,
            ),
            BannerParam(
                message = "A banner with a decorative icon.",
                icon = LucideIcons.WifiOff,
            ),
        )
}

@Preview
@Composable
private fun BannerPreview(
    @PreviewParameter(MessagePreviewParameterProvider::class)
    param: BannerParam,
) {
    TaskfolioThemedPreview {
        Banner(message = param.message, icon = param.icon) { }
    }
}
