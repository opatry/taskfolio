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
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.about.libraries)
}

val versionCodeValue = System.getenv("CI_BUILD_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "net.opatry.tasks.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.opatry.tasks.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = versionCodeValue
        versionName = libs.versions.tasksApp.name.get()

        androidResources.localeFilters += listOf("en", "fr")

        testInstrumentationRunner = "net.opatry.tasks.app.test.TaskfolioTestRunner"
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    flavorDimensions += "target"
    productFlavors {
        create("dev") {
            isDefault = true
            applicationIdSuffix = ".dev"
            dimension = "target"

            manifestPlaceholders["crashlyticsEnabled"] = false
        }

        create("store") {
            dimension = "target"
        }
    }

    signingConfigs {
        create("dev") {
            storeFile = file("dev.keystore")
            storePassword = "devdev"
            keyAlias = "dev"
            keyPassword = "devdev"
        }
        create("store") {
            val keystoreFilePath = findProperty("playstore.keystore.file") as? String
            storeFile = keystoreFilePath?.let(::file)
            storePassword = findProperty("playstore.keystore.password") as? String
            keyAlias = "tasksApp_android"
            keyPassword = findProperty("playstore.keystore.key_password") as? String
        }
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["crashlyticsEnabled"] = false

            signingConfig = signingConfigs.getByName("dev")
        }
        getByName("release") {
            manifestPlaceholders["crashlyticsEnabled"] = true

            // we allow dev signing config in release build when not in CI to allow release builds on dev machine
            val ciBuild = (findProperty("ci") as? String).toBoolean()
            signingConfig = if (signingConfigs.getByName("store").storeFile == null && !ciBuild) {
                signingConfigs.getByName("dev")
            } else {
                signingConfigs.getByName("store")
            }

            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    lint {
        checkDependencies = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        because("requires Dispatchers.Main & co at runtime for Android")
        // java.lang.IllegalStateException: Module with the Main dispatcher is missing. Add dependency providing the Main dispatcher, e.g. 'kotlinx-coroutines-android' and ensure it has the same version as 'kotlinx-coroutines-core'
        // see also https://github.com/JetBrains/compose-jb/releases/tag/v1.1.1
    }

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.androidx.startup)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(compose.material3)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.appcompat)

    implementation(libs.kotlinx.serialization)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    implementation(libs.play.services.auth)

    implementation(project(":google:oauth"))
    implementation(project(":google:tasks"))
    implementation(project(":tasks-app-shared"))

    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
}

aboutLibraries {
    // - If the automatic registered android tasks are disabled, a similar thing can be achieved manually
    // - `./gradlew :tasks-app-android:exportLibraryDefinitions --no-configuration-cache -Pci=true`
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
        outputPath = file("${projectDir}/src/main/assets/licenses_android.json")
        excludeFields.addAll("metadata", "funding", "scm", "associated", "website", "Developer.organisationUrl", "Organization.url")
        prettyPrint = true
        // see available variants: ./gradlew tasks-app-android:outgoingVariants | grep Variant
        exportVariant = "storeRelease"
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