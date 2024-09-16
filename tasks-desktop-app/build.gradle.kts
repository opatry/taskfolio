import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

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

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    application
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.StrongSkipping)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.ktor.server)
            implementation(project(":google:oauth"))
            implementation(project(":google:tasks"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.components.resources)
//            implementation(libs.androidx.compose.material3.adaptive)
//            implementation(libs.androidx.compose.material3.adaptive.layout)
//            implementation(libs.androidx.compose.material3.adaptive.navigation)
//            implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
//            implementation("androidx.compose.material3:material3:1.3.0")
//            implementation("androidx.compose.material3:material3-window-size-class:1.3.0")
//            implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0")
//            implementation(libs.androidx.window.core)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
//            {
//                because("requires Dispatchers.Main & co at runtime for Jvm")
//                // java.lang.IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'
//                // see also https://github.com/JetBrains/compose-jb/releases/tag/v1.1.1
//            }

            implementation(compose.desktop.currentOs)
        }
    }
}
