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

import kotlinx.html.HTML
import kotlinx.html.P
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.noScript
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private object Resources

// FIXME not KMP-friendly
internal fun inlinedResource(resourcePath: String): String {
    return Resources.javaClass.getResource(resourcePath)?.readText() ?: ""
}

// FIXME not KMP-friendly
internal fun inlinedResourceData(resourcePath: String): ByteArray {
    return Resources.javaClass.getResource(resourcePath)?.readBytes() ?: ByteArray(0)
}

@OptIn(ExperimentalEncodingApi::class)
internal fun base64Image(resourcePath: String): String {
    val data = inlinedResourceData(resourcePath)
    return Base64.encode(data)
}

internal fun base64ImageData(resourcePath: String, mimeType: String): String {
    return "data:$mimeType;base64,${base64Image(resourcePath)}"
}

// FIXME branding color not customizable (PNG, SVG, CSS)
fun HTML.authScaffold(
    pageTitle: String,
    subTitle: String = pageTitle,
    illustrationName: String? = null,
    redirectUrl: String? = null,
    message: (P.() -> Unit)? = null
) {
    head {
        title(pageTitle)
        // inline CSS stylesheet instead of using an HTTP served file to let the page be self-contained avoid request on stopped short-lived server
        style(type = "text/css") {
            unsafe {
                // load CSS resource from classpath
                +inlinedResource("/static/style.css")
            }
        }
        link(rel="shortcut icon") {
            type = "image/x-icon"
            href = base64ImageData("/static/favicon.ico", "image/x-icon")
        }
        if (redirectUrl != null) {
            // HTML meta tag to redirect to the app
            meta {
                httpEquiv = "refresh"
                content = "0; url=$redirectUrl"
            }
        }
    }
    body {
        div {
            id = "content"

            h2 {
                +subTitle
            }

            if (illustrationName != null) {
                div("illustration") {
                    p {
                        // base64 SVG image to avoid an HTTP request being made after short-lived server is stopped
                        img(src = base64ImageData("/static/$illustrationName.svg", "image/svg+xml"), alt = "", classes = "centered-image")
                    }

                    noScript {
                        // base64 PNG image to avoid an HTTP request being made after short-lived server is stopped
                        img(src = base64ImageData("/static/$illustrationName.png", "image/png"), alt = "", classes = "centered-image")
                    }
                }
            }

            if (message != null) {
                p {
                    message()
                }
            }
        }
    }
}
