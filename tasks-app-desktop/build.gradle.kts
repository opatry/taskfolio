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

import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.compose.hot.reload)
}

val appName = "Taskfolio"
val appVersion = libs.versions.tasksApp.name.get()
val appVersionCode = System.getenv("CI_BUILD_NUMBER")?.toIntOrNull() ?: 1

compose.resources {
    publicResClass = false
    packageOfResClass = "net.opatry.tasks.app.resources"
    generateResClass = always
}

kotlin {
    jvmToolchain(17)

    dependencies {
        implementation(libs.kotlinx.coroutines.swing) {
            because("requires Dispatchers.Main & co at runtime for Jvm")
            // java.lang.IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'
            // see also https://github.com/JetBrains/compose-jb/releases/tag/v1.1.1
        }

        implementation(libs.kotlinx.datetime)

        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)

        implementation(compose.material3)
        implementation(compose.desktop.currentOs)

        implementation(projects.google.oauth)
        implementation(projects.google.tasks)
        implementation(projects.tasksAppShared)

        testImplementation(kotlin("test"))
        testImplementation(libs.koin.test)
        testImplementation(libs.ktor.client.core) {
            because("needed for Koin DI tests injectedParameters")
        }
        testImplementation(projects.tasksCore) {
            because("needed for Koin DI tests injectedParameters")
        }

        @OptIn(ExperimentalComposeLibrary::class)
        testImplementation(compose.uiTest)
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("MainAppKt")
}

compose.desktop {
    application {
        mainClass = "MainAppKt"
        jvmArgs += listOf(
            "-Dapp.name=$appName",
            "-Dapp.version=$appVersion",
            "-Dapp.version.full=${appVersion}.${appVersionCode}",
        )

        nativeDistributions {
            packageVersion = appVersion
            packageName = appName
            version = appVersion
            description =
                "An Android task management app built using Google Tasks API. Developed to demonstrate my skills in Kotlin multiplatform development."
            copyright = "Copyright (c) 2024-2025 Olivier Patry"
            vendor = "Olivier Patry"
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )

            modules(
                // for org.apache.logging.log4j.core.LoggerContext
                "java.management",
                // for DriverManager (required by SQLite JDBC driver)
                "java.sql",
            )

            buildTypes {
                release {
                    proguard {
                        isEnabled = false
                        // TODO enable proguard
                        //  configurationFiles.from("tasks-app.pro")
                    }
                }
            }

            macOS {
                iconFile.set(project.file("icon.icns"))
                bundleID = "net.opatry.tasks.app"
                infoPlist {
                    extraKeysRawXml = """
  <key>CFBundleURLTypes</key>
  <array>
    <dict>
      <key>CFBundleURLSchemes</key>
      <array>
        <string>taskfolio</string>
      </array>
    </dict>
  </array>
"""
                }
            }
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "Tools"
                shortcut = true
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "7EA468EF-E056-4DFF-8290-C052D9757AC7"
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}

aboutLibraries {
    // - If the automatic registered android tasks are disabled, a similar thing can be achieved manually
    // - `./gradlew :tasks-app-desktop:exportLibraryDefinitions`
    // - the resulting file can for example be added as part of the SCM
    collect {
        configPath = file("$rootDir/license_config")
        offlineMode = true
        fetchRemoteLicense = true
        fetchRemoteFunding = false
        // no need of BOM
        includePlatform = false
    }
    android {
        registerAndroidTasks = false
    }
    export {
        outputPath = file("${projectDir}/src/main/resources/licenses_desktop.json")
        excludeFields.addAll("metadata", "funding", "scm", "associated", "website", "Developer.organisationUrl", "Organization.url")
        prettyPrint = true
    }
    license {
        strictMode = StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "asdkl", "MIT", "EPL-1.0", "BSD-3-Clause")
    }
    library {
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.SIMPLE
    }
}