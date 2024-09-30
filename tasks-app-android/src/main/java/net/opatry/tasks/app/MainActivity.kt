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

package net.opatry.tasks.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import net.opatry.tasks.app.ui.TaskListsViewModel
import net.opatry.tasks.app.ui.TasksApp
import net.opatry.tasks.app.ui.UserState
import net.opatry.tasks.app.ui.UserViewModel
import net.opatry.tasks.app.ui.component.LoadingPane
import net.opatry.tasks.app.ui.screen.AboutApp
import net.opatry.tasks.app.ui.screen.AuthorizationScreen
import net.opatry.tasks.app.ui.theme.TaskfolioTheme
import net.opatry.tasks.app.util.readText
import org.koin.compose.viewmodel.koinViewModel


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkStoreUpdateFlow()

        setContent {
            val userViewModel = koinViewModel<UserViewModel>()
            val userState by userViewModel.state.collectAsState(null)

            if (userState == null) {
                LaunchedEffect(userState) {
                    userViewModel.refreshUserState()
                }
            }

            TaskfolioTheme {
                Surface {
                    when (userState) {
                        null -> LoadingPane()

                        is UserState.Unsigned,
                        is UserState.SignedIn -> {
                            val aboutApp = AboutApp(
                                name = getString(R.string.app_name),
                                version = "${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
                            ) {
                                assets.readText("licenses_android.json")
                            }
                            val tasksViewModel = koinViewModel<TaskListsViewModel>()
                            TasksApp(aboutApp, userViewModel, tasksViewModel)
                        }

                        is UserState.Newcomer -> AuthorizationScreen(
                            onSkip = userViewModel::skipSignIn,
                            onSuccess = userViewModel::signIn,
                        )
                    }
                }
            }
        }
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.data == null) return@registerForActivityResult
        Toast.makeText(this, "Downloading stated", Toast.LENGTH_SHORT).show()
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "Downloading failed", Toast.LENGTH_SHORT).show()
        }
    }
    private val updateResultStarter =
        IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
            val request = IntentSenderRequest.Builder(intent)
                .setFillInIntent(fillInIntent)
                .setFlags(flagsValues, flagsMask)
                .build()
            // launch updateLauncher
            updateLauncher.launch(request)
        }
    val listener = InstallStateUpdatedListener { state ->
        // (Optional) Provide a download progress bar.
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                // Show update progress bar.
            }

            InstallStatus.DOWNLOADED -> {
                Toast.makeText(
                    this,
                    "New app is ready",
                    Toast.LENGTH_SHORT
                ).show()
//                    appUpdateManager.completeUpdate()
            }

            InstallStatus.CANCELED -> Unit
            InstallStatus.FAILED -> Unit
            InstallStatus.INSTALLED -> Unit
            InstallStatus.INSTALLING -> Unit
            InstallStatus.PENDING -> Unit
            InstallStatus.REQUIRES_UI_INTENT -> Unit
            InstallStatus.UNKNOWN -> Unit
        }
    }

    private fun checkStoreUpdateFlow() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(listener)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // TODO appUpdateManager.unregisterListener(listener)
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                when {
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            && appUpdateInfo.updatePriority() >= 4 -> {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateResultStarter,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                            UPDATE_REQUEST_CODE
                        )
                    }

                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                            && appUpdateInfo.clientVersionStalenessDays() ?: -1 >= DAYS_FOR_FLEXIBLE_UPDATE -> {
                    }
                }
            }
        }
    }

    companion object {
        private const val UPDATE_REQUEST_CODE = 1
        private const val DAYS_FOR_FLEXIBLE_UPDATE = 7
    }
}
