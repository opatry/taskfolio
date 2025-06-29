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

package net.opatry.tasks

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToURL

actual class FileCredentialsStorage actual constructor(private val filepath: String) : CredentialsStorage {
    @OptIn(BetaInteropApi::class)
    actual override suspend fun load(): TokenCache? {
        return withContext(Dispatchers.IO) {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(filepath)) return@withContext null

            val data = NSData.dataWithContentsOfFile(filepath)
                ?: return@withContext null

            val content = NSString.create(data, NSUTF8StringEncoding)?.toString()
                ?: return@withContext null

            runCatching {
                Json.decodeFromString<TokenCache>(content)
            }.getOrNull()
        }
    }

    @OptIn(BetaInteropApi::class)
    actual override suspend fun store(tokenCache: TokenCache) {
        val json = Json { prettyPrint = true }

        val success = withContext(Dispatchers.IO) {
            val nsString = NSString.create(string = json.encodeToString(tokenCache))
            val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
                ?: error("Failed to encode JSON to NSData")

            val url = NSURL.fileURLWithPath(filepath)
            data.writeToURL(url, atomically = true)
        }

        if (!success) {
            error("Failed to write token cache to file at $filepath")
        }
    }
}

