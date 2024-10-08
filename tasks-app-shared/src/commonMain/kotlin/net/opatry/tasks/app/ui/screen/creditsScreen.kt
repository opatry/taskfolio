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

import ArrowLeft
import LucideIcons
import SquareArrowOutUpRight
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.credits_screen_license_unknown_authors
import net.opatry.tasks.resources.credits_screen_title
import org.jetbrains.compose.resources.stringResource

private fun Library.authors(max: Int = 3): String? {
    return organization?.name
        ?: developers.mapNotNull(Developer::name)
            .take(max)
            .takeUnless(List<*>::isEmpty)
            ?.joinToString(", ")
}

@Composable
private fun rememberLibraries(block: suspend () -> String): State<Libs?> = produceState<Libs?>(initialValue = null) {
    value = withContext(Dispatchers.Default) {
        Libs.Builder()
            .withJson(block())
            .build()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreenTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(stringResource(Res.string.credits_screen_title))
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(LucideIcons.ArrowLeft, null)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreditsScreenContent(librariesJsonProvider: suspend () -> String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    val aboutLibraries by rememberLibraries(librariesJsonProvider)
    val libraryGroups by remember(aboutLibraries) {
        derivedStateOf {
            aboutLibraries?.libraries
                ?.groupBy(Library::authors)
                ?.toSortedMap { key1, key2 ->
                    when {
                        key1 == null -> 1
                        key2 == null -> -1
                        else -> key1.compareTo(key2)
                    }
                }?.mapValues { (_, libs) ->
                    libs.distinctBy(Library::name).sortedBy(Library::uniqueId)
                }
        }
    }

    LazyColumn(modifier) {
        libraryGroups?.forEach { (authors, libs) ->
            stickyHeader(key = authors) {
                LibraryAuthorsRow(authors ?: stringResource(Res.string.credits_screen_license_unknown_authors))
            }
            items(libs, Library::uniqueId) { lib ->
                LibraryRow(lib, uriHandler::openUri)
            }
        }
    }
}

@Composable
private fun LibraryAuthorsRow(authors: String) {
    Text(
        authors,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryRow(lib: Library, onLicenseClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(lib.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lib.description?.let { description ->
                    Text(
                        description,
                        maxLines = 3,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lib.licenses.forEach { license ->
                        LicenseChip(license) {
                            license.url?.let { onLicenseClick(it) }
                        }
                    }
                }
            }
        },
        trailingContent = lib.artifactVersion?.let { version ->
            {
                Text(version, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun LicenseChip(license: License, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                license.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        trailingIcon = {
            Icon(
                LucideIcons.SquareArrowOutUpRight,
                null,
                Modifier.size(16.dp)
            )
        },
        shape = MaterialTheme.shapes.extraLarge,
    )
}
