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

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.about.libraries)
}

kotlin {
    // kinda useless but need a target to allow sync in IntelliJ
    // and don't want this target to be forced to an iOS one
    // to avoid downloading too much stuff when not needed (no iOS target by default)
    jvm()

    // Note: iOS targets are conditionally added dynamically in the root build.gradle.kts

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":tasks-app-shared"))
        }
    }
}

aboutLibraries {
    // - If the automatic registered android tasks are disabled, a similar thing can be achieved manually
    // - `./gradlew :tasks-app-ios:exportLibraryDefinitions -Pci=true`
    // - the resulting file can for example be added as part of the SCM
    collect {
        configPath = file("$rootDir/license_config")
        offlineMode = true
        fetchRemoteLicense = true
        fetchRemoteFunding = false
        // no need of BOM
        includePlatform = false
    }
    export {
        outputPath = file("$projectDir/Taskfolio/Resources/licenses_ios.json")
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