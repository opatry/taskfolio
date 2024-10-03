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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class NoScreenshot

private fun defaultScreenshotDir() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "test_failed_screenshots")

class ScreenshotOnFailureRule(private val screenshotsDir: File = defaultScreenshotDir()) : TestWatcher() {
    private val Description.allowScreenshot: Boolean
        get() = getAnnotation(NoScreenshot::class.java) == null

    override fun failed(e: Throwable?, description: Description?) {
        description?.let { testDescription ->
            if (testDescription.allowScreenshot) {
                try {
                    takeScreenshot(testDescription)
                } catch (_: Exception) {
                    // ignore screenshot processing errors
                }
            }
        }
        super.failed(e, description)
    }

    private fun takeScreenshot(testDescription: Description) {
        val fileName = testDescription.displayName.take(150)
        val outputFile = File(screenshotsDir, "$fileName.png").also {
            it.parentFile?.mkdirs()
        }
        if (outputFile.exists()) {
            outputFile.delete()
        }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .takeScreenshot(outputFile)
    }
}