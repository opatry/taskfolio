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

import ArrowLeft
import LucideIcons
import SquareArrowOutUpRight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.opatry.tasks.app.ui.tooling.TaskfolioThemedPreview
import net.opatry.tasks.resources.Res
import net.opatry.tasks.resources.credits_screen_license_unknown_authors
import net.opatry.tasks.resources.credits_screen_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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

@Composable
fun CreditsScreenContent(librariesJsonProvider: suspend () -> String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val aboutLibraries by rememberLibraries(librariesJsonProvider)
    CreditsScreenContent(aboutLibraries, modifier, uriHandler::openUri)
}

@Composable
fun CreditsScreenContent(aboutLibraries: Libs?, modifier: Modifier = Modifier, onLicenseClick: (String) -> Unit) {
    val libraryGroups by remember(aboutLibraries) {
        derivedStateOf {
            aboutLibraries?.libraries
                ?.groupBy(Library::authors)
                ?.entries
                ?.sortedWith(compareBy<Map.Entry<String?, List<Library>>> {
                    it.key == null
                }.thenBy { it.key.orEmpty() })
                ?.associate { entry ->
                    val (key, libs) = entry
                    key to libs.distinctBy(Library::name).sortedBy(Library::uniqueId)
                }
        }
    }

    LazyColumn(modifier) {
        libraryGroups?.forEach { (authors, libs) ->
            stickyHeader(key = authors) {
                LibraryAuthorsRow(authors ?: stringResource(Res.string.credits_screen_license_unknown_authors))
            }
            items(libs, Library::uniqueId) { lib ->
                LibraryRow(lib, onLicenseClick)
            }
        }
    }
}

@Composable
internal fun LibraryAuthorsRow(authors: String) {
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

@Composable
internal fun LibraryRow(lib: Library, onLicenseClick: (String) -> Unit) {
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
                        LicenseChip(license.name) {
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
internal fun LicenseChip(licenseName: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                licenseName,
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

@Preview
@Composable
private fun CreditsScreenPreview() {
    val libs = Libs.Builder()
        .withJson(jsonCredits)
        .build()

    TaskfolioThemedPreview {
        Scaffold(
            topBar = {
                CreditsScreenTopAppBar {}
            }
        ) { innerPadding ->
            CreditsScreenContent(
                aboutLibraries = libs,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                onLicenseClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun LibraryAuthorsRowPreview() {
    TaskfolioThemedPreview {
        LibraryAuthorsRow(
            "Google",
        )
    }
}

@Preview
@Composable
private fun LibraryRowPreview() {
    TaskfolioThemedPreview {
        LibraryRow(
            Library(
                name = "Library Name",
                description = "Description of the library",
                licenses = persistentSetOf(
                    License(
                        name = "Apache-2.0",
                        url = null,
                        year = null,
                        spdxId = null,
                        licenseContent = null,
                        hash = "",
                    )
                ),
                artifactVersion = "1.0.0",
                developers = persistentListOf(),
                uniqueId = "",
                website = null,
                scm = null,
                organization = null,
            )
        ) { }
    }
}

@Preview
@Composable
private fun LicenseChipPreview() {
    TaskfolioThemedPreview {
        LicenseChip("Apache-2.0") { }
    }
}

private const val jsonCredits = """
{
  "libraries": [
      {
          "uniqueId": "androidx.activity:activity",
          "artifactVersion": "1.11.0-rc01",
          "description": "Provides the base Activity subclass and the relevant hooks to build a composable structure on top.",
          "name": "Activity",
          "website": "https://developer.android.com/jetpack/androidx/releases/activity#1.11.0-rc01",
          "licenses": [ "Apache-2.0" ],
          "organization": {
              "name": "The Android Open Source Project"
          }
      },
      {
          "uniqueId": "org.jetbrains.kotlin:kotlin-stdlib-common",
          "artifactVersion": "2.1.20",
          "description": "Kotlin Common Standard Library (legacy, use kotlin-stdlib instead)",
          "name": "Kotlin Stdlib Common",
          "website": "https://kotlinlang.org/",
          "licenses": [ "Apache-2.0" ]
      },
      {
          "uniqueId": "org.slf4j:slf4j-nop",
          "artifactVersion": "2.0.17",
          "description": "SLF4J NOP Provider",
          "name": "SLF4J NOP Provider",
          "website": "http://www.slf4j.org",
          "licenses": [ "MIT" ]
      }
  ],
  "licenses": {
      "Apache-2.0": {
          "content": "Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\nTERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION\n\n1. Definitions.\n\n\"License\" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.\n\n\"Licensor\" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.\n\n\"Legal Entity\" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, \"control\" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.\n\n\"You\" (or \"Your\") shall mean an individual or Legal Entity exercising permissions granted by this License.\n\n\"Source\" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.\n\n\"Object\" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.\n\n\"Work\" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).\n\n\"Derivative Works\" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.\n\n\"Contribution\" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, \"submitted\" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as \"Not a Contribution.\"\n\n\"Contributor\" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.\n\n2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.\n\n3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work, where such license applies only to those patent claims licensable by such Contributor that are necessarily infringed by their Contribution(s) alone or by combination of their Contribution(s) with the Work to which such Contribution(s) was submitted. If You institute patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Work or a Contribution incorporated within the Work constitutes direct or contributory patent infringement, then any patent licenses granted to You under this License for that Work shall terminate as of the date such litigation is filed.\n\n4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:\n\n     (a) You must give any other recipients of the Work or Derivative Works a copy of this License; and\n\n     (b) You must cause any modified files to carry prominent notices stating that You changed the files; and\n\n     (c) You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work, excluding those notices that do not pertain to any part of the Derivative Works; and\n\n     (d) If the Work includes a \"NOTICE\" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file, excluding those notices that do not pertain to any part of the Derivative Works, in at least one of the following places: within a NOTICE text file distributed as part of the Derivative Works; within the Source form or documentation, if provided along with the Derivative Works; or, within a display generated by the Derivative Works, if and wherever such third-party notices normally appear. The contents of the NOTICE file are for informational purposes only and do not modify the License. You may add Your own attribution notices within Derivative Works that You distribute, alongside or as an addendum to the NOTICE text from the Work, provided that such additional attribution notices cannot be construed as modifying the License.\n\n     You may add Your own copyright statement to Your modifications and may provide additional or different license terms and conditions for use, reproduction, or distribution of Your modifications, or for any such Derivative Works as a whole, provided Your use, reproduction, and distribution of the Work otherwise complies with the conditions stated in this License.\n\n5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions. Notwithstanding the above, nothing herein shall supersede or modify the terms of any separate license agreement you may have executed with Licensor regarding such Contributions.\n\n6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor, except as required for reasonable and customary use in describing the origin of the Work and reproducing the content of the NOTICE file.\n\n7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.\n\n8. Limitation of Liability. In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, unless required by applicable law (such as deliberate and grossly negligent acts) or agreed to in writing, shall any Contributor be liable to You for damages, including any direct, indirect, special, incidental, or consequential damages of any character arising as a result of this License or out of the use or inability to use the Work (including but not limited to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any and all other commercial damages or losses), even if such Contributor has been advised of the possibility of such damages.\n\n9. Accepting Warranty or Additional Liability. While redistributing the Work or Derivative Works thereof, You may choose to offer, and charge a fee for, acceptance of support, warranty, indemnity, or other liability obligations and/or rights consistent with this License. However, in accepting such obligations, You may act only on Your own behalf and on Your sole responsibility, not on behalf of any other Contributor, and only if You agree to indemnify, defend, and hold each Contributor harmless for any liability incurred by, or claims asserted against, such Contributor by reason of your accepting any such warranty or additional liability.\n\nEND OF TERMS AND CONDITIONS\n\nAPPENDIX: How to apply the Apache License to your work.\n\nTo apply the Apache License to your work, attach the following boilerplate notice, with the fields enclosed by brackets \"[]\" replaced with your own identifying information. (Don't include the brackets!)  The text should be enclosed in the appropriate comment syntax for the file format. We also recommend that a file or class name and description of purpose be included on the same \"printed page\" as the copyright notice for easier identification within third-party archives.\n\nCopyright [yyyy] [name of copyright owner]\n\nLicensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\nhttp://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS,\nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\nSee the License for the specific language governing permissions and\nlimitations under the License.",
          "hash": "Apache-2.0",
          "internalHash": "Apache-2.0",
          "url": "https://spdx.org/licenses/Apache-2.0.html",
          "spdxId": "Apache-2.0",
          "name": "Apache License 2.0"
      },
      "MIT": {
          "content": "MIT License\n\nCopyright (c) <year> <copyright holders>\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.",
          "hash": "MIT",
          "internalHash": "MIT",
          "url": "https://spdx.org/licenses/MIT.html",
          "spdxId": "MIT",
          "name": "MIT License"
      }
  }
}
"""
