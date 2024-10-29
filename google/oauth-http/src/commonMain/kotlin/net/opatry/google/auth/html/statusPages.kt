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

package net.opatry.google.auth.html

import io.ktor.http.HttpStatusCode
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import kotlinx.html.code

fun StatusPagesConfig.notFound() {
    status(HttpStatusCode.NotFound) { call, code ->
        call.respondHtml(code) {
            authScaffold(
                pageTitle = "Resource not found",
                illustrationName = "undraw_page_not_found_re_e9o6"
            ) {
                +"Can't find the requested resource."
            }
        }
    }
}

fun StatusPagesConfig.internalError() {
    exception<Throwable> { call, cause ->
        call.respondHtml(HttpStatusCode.InternalServerError) {
            authScaffold(
                pageTitle = "Internal server error",
                illustrationName = "undraw_fixing_bugs_w7gi"
            ) {
                +"An unexpected error occurred ("
                code {
                    +(cause.message ?: "unknown error")
                }
                +")."
            }
        }
    }
}