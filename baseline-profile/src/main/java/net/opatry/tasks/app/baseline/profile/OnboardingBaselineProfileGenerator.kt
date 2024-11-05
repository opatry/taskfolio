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

package net.opatry.tasks.app.baseline.profile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class OnboardingBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateOnboardingBaselineProfile() {
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: error("targetAppId not passed as instrumentation runner arg"),
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()

//            val authorizationSkipButtonRes = By.res("authorization.skip_button")
//            device.wait(Until.hasObject(authorizationSkipButtonRes), 500)
//            if (iteration == 1 || device.hasObject(authorizationSkipButtonRes)) {
//                device.findObject(authorizationSkipButtonRes)?.click()
//            }
            device.wait(Until.hasObject(By.text("Add task list…")), 5000)
            device.wait(Until.hasObject(By.text("Mes tâches")), 5000)
        }
    }
}