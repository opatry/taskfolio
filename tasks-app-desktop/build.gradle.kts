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

import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.about.libraries)
}

val appVersion = libs.versions.tasksApp.name.get()
val appVersionCode = System.getenv("CI_BUILD_NUMBER")?.toIntOrNull() ?: 1

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

        implementation(project(":google:oauth"))
        implementation(project(":google:tasks"))
        implementation(project(":tasks-app-shared"))
    }
}

compose.desktop {
    application {
        mainClass = "MainAppKt"
        jvmArgs += listOf(
            "-Dapp.version=$appVersion",
            "-Dapp.version.full=${appVersion}.${appVersionCode}",
        )
        nativeDistributions {
            packageVersion = appVersion
            packageName = "Taskfolio"
            version = appVersion
            targetFormats(
                TargetFormat.Dmg,
            )

            modules(
                // for org.apache.logging.log4j.core.LoggerContext
                "java.management",
                // for DriverManager (required by SQLite JDBC driver)
                "java.sql",
            )

            macOS {
                bundleID = "net.opatry.tasks.app"
            }
        }
    }
}

aboutLibraries {
    // - If the automatic registered android tasks are disabled, a similar thing can be achieved manually
    // - `./gradlew :tasks-app-desktop:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/resources`
    // - the resulting file can for example be added as part of the SCM
    registerAndroidTasks = false
    outputFileName = "licenses_desktop.json"
    // Define the path configuration files are located in. E.g. additional libraries, licenses to add to the target .json
    // Warning: Do not use the parent folder of a module as path (see https://github.com/mikepenz/AboutLibraries/issues/936)
    configPath = "license_config"
    offlineMode = true
    fetchRemoteLicense = true
    fetchRemoteFunding = false
    excludeFields = arrayOf("metadata", "funding", "scm", "associated", "website", "Developer.organisationUrl", "Organization.url")
    includePlatform = true
    strictMode = StrictMode.FAIL
    allowedLicenses = arrayOf("Apache-2.0", "asdkl", "MIT")
    duplicationMode = DuplicateMode.LINK
    duplicationRule = DuplicateRule.SIMPLE
    prettyPrint = true
}