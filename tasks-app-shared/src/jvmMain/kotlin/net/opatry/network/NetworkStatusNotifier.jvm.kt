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

package net.opatry.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLConnection
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

private fun isInternetAvailableURLConnectionImpl(): Boolean = try {
    val url = URI.create("https://clients3.google.com/generate_204").toURL()
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "HEAD"
        connectTimeout = 500
        readTimeout = 500
    }.also(URLConnection::connect)
    connection.responseCode in 200..<400
} catch (_: Exception) {
    false
}

class NetworkStatusNotifier(
    private val dispatcher: CoroutineContext = Dispatchers.IO,
    private val checkNetwork: () -> Boolean = ::isInternetAvailableURLConnectionImpl,
) {
    companion object {
        private val PollingDelay = 5.seconds
        private val MaxPollingDelay = 15.seconds
    }

    fun networkStateFlow(): Flow<Boolean> = flow {
        var wasAvailable: Boolean? = null
        var pollingDelay = PollingDelay

        while (currentCoroutineContext().isActive) {
            val isAvailable = checkNetwork()

            pollingDelay = if (isAvailable != wasAvailable) {
                emit(isAvailable)
                wasAvailable = isAvailable
                PollingDelay
            } else {
                when {
                    isAvailable -> PollingDelay
                    // exponential backoff when network is not available
                    else -> (pollingDelay * 2).coerceAtMost(MaxPollingDelay)
                }
            }

            delay(pollingDelay)
        }
    }.flowOn(dispatcher)
}

private val notifier = NetworkStatusNotifier()
actual fun networkStateFlow(): Flow<Boolean> = notifier.networkStateFlow()
