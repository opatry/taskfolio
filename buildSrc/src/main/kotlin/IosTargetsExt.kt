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

import org.gradle.api.Project
import java.io.File
import java.util.*

// can't use by lazy, we need Project.findProperty not accessible there
@Suppress("ObjectPropertyName")
private lateinit var _iosTargets: List<String>

private val localProperties = Properties()
private fun Project.getIosTargetedConfiguration(): String? {
    return findProperty("ios.target") as? String
        ?: System.getenv("IOS_TARGET")
        ?: run {
            if (localProperties.isEmpty) {
                val localPropertiesFile = File(rootDir, "local.properties")
                if (localPropertiesFile.isFile) {
                    localPropertiesFile.inputStream().use { reader ->
                        localProperties.load(reader)
                    }
                }
            }
            localProperties.getProperty("ios.target")
        }
}

val Project.iosTargets: List<String>
    get() {
        if (!::_iosTargets.isInitialized) {
            _iosTargets = when (getIosTargetedConfiguration()) {
                // We ignore "iosX64", not considered as a use case
                "all" -> listOf("iosArm64", "iosSimulatorArm64")
                "simulator" -> listOf("iosSimulatorArm64")
                "device" -> listOf("iosArm64")
                "none" -> emptyList()
                else -> emptyList()
            }
        }
        return _iosTargets
    }
