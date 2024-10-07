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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier


@Composable
actual fun AboutScreen(aboutApp: AboutApp) {
    var currentDestination by remember { mutableStateOf(AboutScreenDestination.About) }

    BackHandler(currentDestination != AboutScreenDestination.About) {
        // TODO doesn't support deep navigation for now, would need a real "navigator.navigateBack()" if needed
        //  kept simple for now, just handle Android native back to leave Credits screen
        currentDestination = AboutScreenDestination.About
    }

    Scaffold(topBar = {
        when (currentDestination) {
            AboutScreenDestination.Credits -> CreditsScreenTopAppBar {
                currentDestination = AboutScreenDestination.About
            }
            AboutScreenDestination.About ->  AboutScreenTopAppBar(aboutApp.name, aboutApp.version)
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
