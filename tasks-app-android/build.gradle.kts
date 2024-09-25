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
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "net.opatry.tasks.app"
    compileSdk = 34
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.opatry.tasks.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.tasksApp.code.get().toInt()
        versionName = libs.versions.tasksApp.name.get()

        resourceConfigurations += listOf("en", "fr")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    signingConfigs {
        create("dev") {
            storeFile = file("dev.keystore")
            storePassword = "devdev"
            keyAlias = "dev"
            keyPassword = "devdev"
        }
        create("prod") {
            val keystoreFilePath = findProperty("playstore.keystore.file") as? String
            storeFile = keystoreFilePath?.let(::file)
            storePassword = findProperty("playstore.keystore.password") as? String
            keyAlias = "tasksApp_android"
            keyPassword = findProperty("playstore.keystore.key_password") as? String
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("dev")
        }
        getByName("release") {
            // we allow dev signing config in release build when not in CI to allow release builds on dev machine
            val ciBuild = (findProperty("ci") as? String)?.toBoolean() ?: false
            signingConfig = if (signingConfigs.getByName("prod").storeFile == null && !ciBuild) {
                signingConfigs.getByName("dev")
            } else {
                signingConfigs.getByName("prod")
            }

            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    lint {
        checkDependencies = true
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.pickFirsts += listOf(
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties",
        )
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android) {
        because("requires Dispatchers.Main & co at runtime for Jvm")
        // java.lang.IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'
        // see also https://github.com/JetBrains/compose-jb/releases/tag/v1.1.1
    }

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(compose.material3)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.appcompat)

    implementation(project(":google:oauth"))
    implementation(project(":google:tasks"))
    implementation(project(":tasks-app-shared"))
}