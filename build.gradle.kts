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

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.jetbrains.kotlin.compose.compiler) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.about.libraries) apply false
    alias(libs.plugins.kover)
}

val koverProjects = listOf(
    project(":tasks-app-shared"),
    project(":tasks-core"),
    project(":google:tasks"),
)

dependencies {
    koverProjects.onEach { koverProject ->
        kover(koverProject)
    }
}

val koverExcludedClasses = listOf(
    "net.opatry.logging.*",
    "net.opatry.logging.PrintLogger*",
    "net.opatry.network.NetworkStatusNotifier_*",
    "net.opatry.network.*_androidKt",
    "net.opatry.google.tasks.model.*",
    "net.opatry.google.profile.UserInfoErrorResponse*",
    "net.opatry.tasks.TokenCache*",
    "net.opatry.tasks.FileCredentialsStorage*",
    "net.opatry.tasks.app.auth.*",
    "net.opatry.tasks.app.auth.PlayServicesGoogleAuthenticator*",
    "net.opatry.tasks.app.di.*",
    "net.opatry.tasks.app.di.*_androidKt",
    "net.opatry.tasks.app.di.*ModuleKt",
    "net.opatry.tasks.app.presentation.model.*",
    "net.opatry.tasks.app.ui.AppTasksScreen*",
    "net.opatry.tasks.app.ui.Singletons*",
    "net.opatry.tasks.app.ui.ComposableSingletons*",
    "net.opatry.tasks.app.ui.TaskEvent*",
    "net.opatry.tasks.app.ui.TasksApp*",
    "net.opatry.tasks.app.ui.*PreviewKt*",
    "net.opatry.tasks.app.ui.*PreviewParameterProvider*",
    "net.opatry.tasks.app.ui.*PreviewDataProvider*",
    "net.opatry.tasks.app.ui.*PreviewData*",
    "net.opatry.tasks.app.ui.component.LoadingIndicator*",
    "net.opatry.tasks.app.ui.component.LoadingPane*",
    "net.opatry.tasks.app.ui.component.MissingScreen*",
    "net.opatry.tasks.app.ui.component.ComposableSingletons*",
    "net.opatry.tasks.app.ui.component.AuthorizeGoogleTasksButton*",
    "net.opatry.tasks.app.ui.component.BackHandler*",
    "net.opatry.tasks.app.ui.component.EmptyStateParam*",
    "net.opatry.tasks.app.ui.component.BannerParam*",
    "net.opatry.tasks.app.ui.component.*PreviewParameterProvider",
    "net.opatry.tasks.app.ui.component.*Preview\$1*",
    "net.opatry.tasks.app.ui.icon.*",
    "net.opatry.tasks.app.ui.screen.*",
    "net.opatry.tasks.app.ui.theme.*",
    "net.opatry.tasks.app.ui.tooling.*",
    "net.opatry.tasks.data.*Dao",
    "net.opatry.tasks.data.*Dao\$DefaultImpls",
    "net.opatry.tasks.data.Converters",
    "net.opatry.tasks.data.entity.*",
    "net.opatry.tasks.data.model.*",
    "net.opatry.tasks.data.TasksAppDatabase*",
    "net.opatry.tasks.resources.*ResourceCollectorsKt*",
    "net.opatry.tasks.resources.Res*",
    "net.opatry.tasks.resources.*_commonMainKt*",
)

kover {
    currentProject {
        createVariant("coverage") {}
    }

    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                classes(*koverExcludedClasses.toTypedArray())
                annotatedBy("*Preview*")
            }
        }

        variant("coverage") {
            verify {
                rule("Instruction coverage") {
                    minBound(
                        minValue = 80,
                        coverageUnits = CoverageUnit.INSTRUCTION
                    )
                }
                rule("Line coverage") {
                    minBound(
                        minValue = 85,
                        coverageUnits = CoverageUnit.LINE
                    )
                }
            }
        }
    }
}

private val kmpPluginId = libs.plugins.jetbrains.kotlin.multiplatform.get().pluginId
subprojects {
    plugins.withId(kmpPluginId) {
        if (project == project(":google:oauth-http")) return@withId

        extensions.configure<KotlinMultiplatformExtension> {
            // foo-bar-zorg → FooBarZorg
            val frameworkBaseName = project.name.split('-').joinToString("") { part ->
                part.replaceFirstChar(Char::uppercase)
            }
            iosTargets.mapNotNull {
                when (it) {
                    "iosX64" -> iosX64()
                    "iosArm64" -> iosArm64()
                    "iosSimulatorArm64" -> iosSimulatorArm64()
                    else -> null
                }
            }.forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = frameworkBaseName
                    isStatic = true
                }
            }
        }
    }

    tasks {
        findByName("test") ?: return@tasks
        named<Test>("test") {
            testLogging {
                events("failed", "passed", "skipped")

                exceptionFormat = TestExceptionFormat.SHORT

                debug {
                    exceptionFormat = TestExceptionFormat.FULL
                }
            }
        }
    }

    if (project in koverProjects) {
        project.afterEvaluate {
            apply(plugin = libs.plugins.kover.get().pluginId)
            kover {
                currentProject {
                    createVariant("coverage") {
                        add("jvm")
                        addWithDependencies("debug", optional = true)
                    }
                }
                reports {
                    filters {
                        excludes {
                            androidGeneratedClasses()
                            classes(*koverExcludedClasses.toTypedArray())
                            annotatedBy("*Preview*")
                        }
                    }
                }
            }
        }
    }
}

gradle.projectsEvaluated {
    val xcFrameworkTask = project(":tasks-app-shared").tasks.findByName("embedAndSignAppleFrameworkForXcode")
    val updateVersionTask = project(":tasks-app-ios").tasks.findByName("updateXcodeVersionConfig")

    if (xcFrameworkTask != null && updateVersionTask != null) {
        xcFrameworkTask.dependsOn(updateVersionTask)
    }
}
