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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import net.opatry.tasks.app.ui.component.MyBackHandler
import net.opatry.tasks.app.ui.icon.CheckCircle
import net.opatry.tasks.app.ui.icon.MaterialSymbols
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.about_screen_app_version_subtitle
import net.opatry.tasks.resources.about_screen_credits_item
import net.opatry.tasks.resources.about_screen_github_item
import net.opatry.tasks.resources.about_screen_privacy_policy_item
import net.opatry.tasks.resources.about_screen_website_item
import org.jetbrains.compose.resources.stringResource


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
fun AboutScreen(aboutApp: AboutApp) {
    var currentDestination by remember { mutableStateOf(AboutScreenDestination.About) }

    MyBackHandler({ currentDestination != AboutScreenDestination.About }) {
        // TODO doesn't support deep navigation for now, would need a real "navigator.navigateBack()" if needed
        //  kept simple for now, just handle Android native back to leave Credits screen
        currentDestination = AboutScreenDestination.About
    }

    Scaffold(topBar = {
        when (currentDestination) {
            AboutScreenDestination.Credits -> CreditsScreenTopAppBar {
                currentDestination = AboutScreenDestination.About
            }

            AboutScreenDestination.About -> AboutScreenTopAppBar(aboutApp.name, aboutApp.version)
        }
    }) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AboutScreenDestination.About -> AboutScreenContent(aboutApp) { destination ->
                    currentDestination = destination
                }

                AboutScreenDestination.Credits -> CreditsScreenContent(aboutApp.aboutLibrariesJsonProvider)
            }
        }
    }
}

private const val TASKFOLIO_WEBSITE_URL = "https://opatry.github.io/taskfolio/"
private const val TASKFOLIO_GITHUB_URL = "https://github.com/opatry/taskfolio"
private const val TASKFOLIO_PRIVACY_POLICY_URL = "https://opatry.github.io/taskfolio/privacy-policy"

@Composable
fun AboutScreenContent(aboutApp: AboutApp, onNavigate: (AboutScreenDestination) -> Unit) {
    val uriHandler = LocalUriHandler.current

    LazyColumn {
        item {
            AboutExternalLink(stringResource(Res.string.about_screen_website_item), LucideIcons.Earth) {
                uriHandler.openUri(TASKFOLIO_WEBSITE_URL)
            }
        }
        item {
            AboutExternalLink(stringResource(Res.string.about_screen_github_item), LucideIcons.Github) {
                uriHandler.openUri(TASKFOLIO_GITHUB_URL)
            }
        }
        item {
            AboutExternalLink(stringResource(Res.string.about_screen_privacy_policy_item), LucideIcons.ShieldCheck) {
                uriHandler.openUri(TASKFOLIO_PRIVACY_POLICY_URL)
            }
        }
        item {
            ListItem(
                modifier = Modifier.clickable(onClick = { onNavigate(AboutScreenDestination.Credits) }),
                leadingContent = { Icon(LucideIcons.Copyright, null) },
                headlineContent = { Text(stringResource(Res.string.about_screen_credits_item)) },
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
                // FIXME How to style only the version part taking into account localization?
                Text(stringResource(Res.string.about_screen_app_version_subtitle, appVersion), style = MaterialTheme.typography.labelSmall)
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
            MaterialSymbols.CheckCircle,
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
