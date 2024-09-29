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

import android.app.Application
import net.opatry.tasks.app.di.authModule
import net.opatry.tasks.app.di.dataModule
import net.opatry.tasks.app.di.networkModule
import net.opatry.tasks.app.di.platformModule
import net.opatry.tasks.app.di.tasksAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup.onKoinStartup

private const val GCP_CLIENT_ID = "191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com"

class TasksApplication : Application() {

    init {
        @Suppress("OPT_IN_USAGE")
        onKoinStartup {
            androidContext(this@TasksApplication)
            modules(
                platformModule(BuildConfig.FLAVOR),
                dataModule,
                authModule(GCP_CLIENT_ID),
                networkModule,
                tasksAppModule,
            )
        }
    }
}