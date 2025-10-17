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

package net.opatry.tasks.app

import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.tasks.app.di.dataModule
import net.opatry.tasks.app.di.loggingModule
import net.opatry.tasks.app.di.networkModule
import net.opatry.tasks.app.di.platformModule
import net.opatry.tasks.app.di.tasksAppModule
import net.opatry.tasks.app.di.utilModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

@Suppress("unused")
fun initKoin(googleAuthenticator: GoogleAuthenticator) {
    startKoin {
        modules(
            utilModule,
            loggingModule,
            platformModule("ios"),
            dataModule,
            module {
                single { googleAuthenticator }
            },
            networkModule,
            tasksAppModule,
        )
    }
}
