/*
 * Copyright (c) 2024 Olivier Patry
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

package net.opatry.tasks.app.ui.screen

import ChevronRight
import Copyright
import Earth
import Github
import LucideIcons
import ShieldCheck
import SquareArrowOutUpRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp


data class AboutApp(
    val name: String,
    val version: String,
    val aboutLibrariesJsonProvider: suspend () -> String
)

enum class AboutScreenDestination {
    About,
    Credits,
}

@Composable
expect fun AboutScreen(aboutApp: AboutApp)

@Composable
fun AboutScreenContent(aboutApp: AboutApp, onNavigate: (AboutScreenDestination) -> Unit) {
    val uriHandler = LocalUriHandler.current

    LazyColumn {
        item {
            AboutExternalLink("Website", LucideIcons.Earth) {
                uriHandler.openUri("https://opatry.github.io/taskfolio")
            }
        }
        item {
            AboutExternalLink("Github", LucideIcons.Github) {
                uriHandler.openUri("https://github.com/opatry/taskfolio")
            }
        }
        item {
            AboutExternalLink("Privacy Policy", LucideIcons.ShieldCheck) {
                uriHandler.openUri("https://opatry.github.io/taskfolio/privacy-policy")
            }
        }
        item {
            ListItem(
                modifier = Modifier.clickable(onClick = { onNavigate(AboutScreenDestination.Credits) }),
                leadingContent = { Icon(LucideIcons.Copyright, null) },
                headlineContent = { Text("Credits") },
                trailingContent = { Icon(LucideIcons.ChevronRight, null) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreenTopAppBar(appName: String, appVersion: String) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(appName)
                Text(buildAnnotatedString {
                    append("Version ")
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(appVersion)
                    }
                }, style = MaterialTheme.typography.labelSmall)
            }
        },
        navigationIcon = { AppIcon() }
    )
}

@Composable
private fun AppIcon() {
    // TODO app icon in compose res
    Box(
        Modifier
            .padding(8.dp)
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(Color(0xFF023232)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            null,
            Modifier.fillMaxSize(.7f),
            tint = Color(0xFF81FFDE)
        )
    }
}

@Composable
private fun AboutExternalLink(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            if (icon != null) {
                Icon(icon, null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        headlineContent = { Text(label) },
        trailingContent = { Icon(LucideIcons.SquareArrowOutUpRight, null) },
    )
}
