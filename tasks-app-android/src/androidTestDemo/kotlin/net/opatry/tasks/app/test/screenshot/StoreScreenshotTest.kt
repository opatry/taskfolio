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

package net.opatry.tasks.app.test.screenshot

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.test.runTest
import net.opatry.tasks.app.MainActivity
import net.opatry.tasks.app.R
import net.opatry.tasks.app.test.ScreenshotOnFailureRule
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.NOTES_FIELD
import net.opatry.tasks.app.ui.component.TaskEditorBottomSheetTestTag.TITLE_FIELD
import net.opatry.tasks.app.ui.component.TaskListScaffoldTestTag.ADD_TASK_FAB
import net.opatry.tasks.app.ui.component.TasksColumnTestTag.COMPLETED_TASKS_TOGGLE
import org.junit.Rule
import org.junit.Test
import java.io.File


@OptIn(ExperimentalTestApi::class)
class StoreScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val screenshotOnFailureRule = ScreenshotOnFailureRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun takeScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputDir = File(instrumentation.targetContext.cacheDir, "store_screenshots").also(File::mkdirs)
        val outputFile = File(outputDir, "$name.png")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        UiDevice.getInstance(instrumentation)
            .takeScreenshot(outputFile)
    }

    private fun pressBack() {
        // FIXME how to "press back" with ComposeTestRule (without Espresso)
        //  UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        //  UI Automator doesn't work for navigation (but does for IME dismiss)
        composeTestRule.activity.onBackPressed()
    }

    private fun dismissKeyboard() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }

    private fun switchToNightMode(nightMode: Int) {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.delegate.localNightMode = nightMode
        }
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    /**
     * This test should be executed with the `demo` flavor which stub content for store screenshots.
     */
    @Test
    fun storeScreenshotSequence() = runTest {
        val initialNightMode = composeTestRule.activity.delegate.localNightMode

        composeTestRule.waitForIdle()
        takeScreenshot("initial_screen")

        switchToNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val defaultTaskTitle = targetContext.getString(R.string.demo_task_list_default)
        composeTestRule.waitUntilAtLeastOneExists(hasText(defaultTaskTitle))
        composeTestRule.onNodeWithText(defaultTaskTitle)
            .assertIsDisplayed()

        val homeTaskTitle = targetContext.getString(R.string.demo_task_list_home)
        composeTestRule.onNodeWithText(homeTaskTitle)
            .assertIsDisplayed()

        val groceriesTaskTitle = targetContext.getString(R.string.demo_task_list_groceries)
        composeTestRule.onNodeWithText(groceriesTaskTitle)
            .assertIsDisplayed()

        val workTaskTitle = targetContext.getString(R.string.demo_task_list_work)
        composeTestRule.onNodeWithText(workTaskTitle)
            .assertIsDisplayed()

        takeScreenshot("task_lists_light")

        composeTestRule.onNodeWithText(defaultTaskTitle)
            .assertIsDisplayed()
            .performClick()
        val defaultTask1Title = targetContext.getString(R.string.demo_task_list_default_task1)
        composeTestRule.waitUntilAtLeastOneExists(hasText(defaultTask1Title))
        // FIXME unreliable, need to wait for something else?
        takeScreenshot("my_tasks_light")

        composeTestRule.waitUntilExactlyOneExists(hasTestTag(ADD_TASK_FAB))
        composeTestRule.onNodeWithTag(ADD_TASK_FAB)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitUntilExactlyOneExists(isDialog())

        composeTestRule.waitUntilExactlyOneExists(hasTestTag(TITLE_FIELD))
        composeTestRule.onNodeWithTag(TITLE_FIELD)
            .performTextInput("Wash the car 🧽")
        composeTestRule.waitForIdle()
        dismissKeyboard()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag(NOTES_FIELD))
        composeTestRule.onNodeWithTag(NOTES_FIELD)
            .performTextInput("Keys are in the drawer")

        dismissKeyboard()

        composeTestRule.waitForIdle()
        takeScreenshot("add_task_light")

        // FIXME how to dismiss bottom sheet without clicking on the button? (press back somehow? tap outside?)
        // FIXME how to use Res strings from :tasks-app-shared?
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            .performClick()
        // go back
        pressBack()
        composeTestRule.waitUntilAtLeastOneExists(hasText(groceriesTaskTitle))

        composeTestRule.onNodeWithText(groceriesTaskTitle)
            .assertIsDisplayed()
            .performClick()
        val groceriesTask1Title = targetContext.getString(R.string.demo_task_list_groceries_task1)
        composeTestRule.waitUntilAtLeastOneExists(hasText(groceriesTask1Title))
        composeTestRule.onNodeWithTag(COMPLETED_TASKS_TOGGLE)
            .assertIsDisplayed()
            .performClick()
        val groceriesTask3Title = targetContext.getString(R.string.demo_task_list_groceries_task3)
        composeTestRule.waitUntilAtLeastOneExists(hasText(groceriesTask3Title))
        takeScreenshot("groceries_light")

        pressBack()
        composeTestRule.waitUntilAtLeastOneExists(hasText(workTaskTitle))

        composeTestRule.onNodeWithText(workTaskTitle)
            .assertIsDisplayed()
            .performClick()
        val workTask1Title = targetContext.getString(R.string.demo_task_list_work_task1)
        composeTestRule.waitUntilAtLeastOneExists(hasText(workTask1Title))
        takeScreenshot("work_light")

        pressBack()
        composeTestRule.waitUntilAtLeastOneExists(hasText(homeTaskTitle))

        composeTestRule.onNodeWithText(homeTaskTitle)
            .assertIsDisplayed()
            .performClick()
        val homeTask1Title = targetContext.getString(R.string.demo_task_list_home_task1)
        composeTestRule.waitUntilAtLeastOneExists(hasText(homeTask1Title))
        takeScreenshot("home_light")

        switchToNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        composeTestRule.waitUntilAtLeastOneExists(hasText(homeTask1Title))
        takeScreenshot("home_dark")
        switchToNightMode(initialNightMode)
    }
}