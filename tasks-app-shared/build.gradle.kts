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

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree


plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.android.library)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.opatry.tasks.resources"
    generateResClass = auto
}

kotlin {
    jvm()

    androidTarget {
        // useful to allow using commonTest in Android instrumentation tests
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    jvmToolchain(17)

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

            implementation(libs.play.services.auth)
        }

        jvmMain.dependencies {
            implementation(projects.google.oauthHttp)
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)

            api(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.slf4j.nop)
            implementation(projects.google.oauth)
            implementation(projects.google.tasks)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            api(compose.components.resources)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(libs.compose.m3.adaptive)
            implementation(libs.compose.m3.adaptive.layout)
            implementation(libs.compose.m3.adaptive.navigation)
            implementation(libs.bundles.coil)

            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)

            implementation(dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.about.libraries.core)

            implementation(projects.tasksCore)

            implementation(projects.lucideIcons)
        }

        // FIXME temporary workaround, SQLite bundled metadata is broken between alpha13 and beta01
        //  explicit Jvm dependency to be removed once beta02 is out
        //  see https://issuetracker.google.com/issues/396148592
        jvmMain.dependencies {
            implementation("androidx.sqlite:sqlite-jvm:${libs.versions.sqlite.get()}")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))

            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)

            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.core)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
}

android {
    namespace = "net.opatry.tasks"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    testOptions {
        unitTests {
            all {
                it.exclude("**/ui/**", "**/data/**")
            }
        }
    }
}