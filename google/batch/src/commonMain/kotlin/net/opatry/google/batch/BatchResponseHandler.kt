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

package net.opatry.google.batch

import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HeadersImpl
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.CIOHeaders
import io.ktor.http.cio.CIOMultipartDataBase
import io.ktor.http.cio.ConnectionOptions
import io.ktor.http.cio.HttpHeadersMap
import io.ktor.http.cio.MultipartEvent
import io.ktor.http.cio.internals.MutableRange
import io.ktor.http.cio.parseResponse
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope

// copied from Ktor utils.kt
internal fun HttpHeadersMap.toMap(): Map<String, List<String>> {
    val result = mutableMapOf<String, MutableList<String>>()

    for (index in 0 until size) {
        val key = nameAt(index).toString()
        val value = valueAt(index).toString()

        if (result[key]?.add(value) == null) {
            result[key] = mutableListOf(value)
        }
    }

    return result
}

// copied from Ktor utils.kt
internal fun HttpStatusCode.isInformational(): Boolean = (value / 100) == 1

// copied from Ktor Tokenizer.kt
internal fun skipSpaces(text: CharSequence, range: MutableRange) {
    var idx = range.start
    val end = range.end

    if (idx >= end || !text[idx].isWhitespace()) return
    idx++

    while (idx < end) {
        if (!text[idx].isWhitespace()) break
        idx++
    }

    range.start = idx
}


private suspend fun readPartSuspend(events: ReceiveChannel<MultipartEvent>): PartData? {
    try {
        while (true) {
            val event = events.receive()
            eventToData(event)?.let { return it }
        }
    } catch (t: ClosedReceiveChannelException) {
        return null
    }
}

private suspend fun eventToData(event: MultipartEvent): PartData? {
    return try {
        when (event) {
            is MultipartEvent.MultipartPart -> partToData(event)
            else -> {
                event.release()
                null
            }
        }
    } catch (cause: Throwable) {
        event.release()
        throw cause
    }
}

private suspend fun partToData(part: MultipartEvent.MultipartPart): PartData {
    val headers = part.headers.await()

    val contentDisposition = headers["Content-Disposition"]?.let { ContentDisposition.parse(it.toString()) }
    val filename = contentDisposition?.parameter("filename")

    val body = part.body
    if (filename == null) {
        val packet = body.readRemaining() // formFieldLimit.toLong())
//            if (!body.exhausted()) {
//                val cause = IllegalStateException("Form field size limit exceeded: $formFieldLimit")
//                body.cancel(cause)
//                throw cause
//            }

        packet.use {
            return PartData.FormItem(it.readText(), { part.release() }, CIOHeaders(headers))
        }
    }

    return PartData.FileItem(
        { part.body },
        { part.release() },
        CIOHeaders(headers)
    )
}

@OptIn(InternalAPI::class)
suspend fun handleBatchResponse(response: HttpResponse): List<PartData> {
    return coroutineScope {
        val channel = response.bodyAsChannel()
        val data = CIOMultipartDataBase(Dispatchers.IO, channel, response.contentType().toString(), response.contentLength())

        buildList {
            data.forEachPart(::add)
        }
    }
}

suspend fun handleBatchResponse2(response: HttpResponse): List<HttpResponseData> {
    val contentType = response.headers[HttpHeaders.ContentType]
        ?: error("No ${HttpHeaders.ContentType} header")
    // FIXME
    val callContext = Dispatchers.Default
    // FIXME beurk
    val requestTime = GMTDate(System.currentTimeMillis())
    when {
        contentType.startsWith("multipart/mixed") -> {
            val boundary = ContentType.parse(contentType).parameter("boundary")
                ?: error("No boundary in ${HttpHeaders.ContentType} header")

            // TODO use response.bodyAsChannel() instead of bodyAsText() and handle split of boundary
            val batchBody = response.bodyAsText().removeSuffix("--$boundary--\r\n")
            return batchBody.split("--$boundary").mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null

                val input = ByteReadChannel(part.trim())
                input.readUTF8Line() // skip first line (ContentType: application/http)
                input.readUTF8Line() // skip empty line
                val rawResponse = parseResponse(input) ?: error("Can't parse response")
                val status = HttpStatusCode(rawResponse.status, rawResponse.statusText.toString())
                val contentLength = rawResponse.headers[HttpHeaders.ContentLength]?.toString()?.toLong() ?: -1L
                val transferEncoding = rawResponse.headers[HttpHeaders.TransferEncoding]?.toString()
                val connectionType = ConnectionOptions.parse(rawResponse.headers[HttpHeaders.Connection])
//                    // FIXME manually added due to parseHttpBody exception
//                    ?: ConnectionOptions.Close

                val rawHeaders = rawResponse.headers
                val headers = HeadersImpl(rawHeaders.toMap())
                val version = HttpProtocolVersion.parse(rawResponse.version)
                // FIXME
                //  if (status == HttpStatusCode.SwitchingProtocols) {
                //    val session = RawWebSocket(input, output, masking = true, coroutineContext = callContext)
                //    return@withContext HttpResponseData(status, requestTime, headers, version, session, callContext)
                //  }

//                val body = when {
//                    // FIXME request.method == HttpMethod.Head ||
//                    status in listOf(HttpStatusCode.NotModified, HttpStatusCode.NoContent) ||
//                            status.isInformational() -> {
//                        ByteReadChannel.Empty
//                    }
//
//                    else -> {
//                        val coroutineScope = CoroutineScope(callContext + CoroutineName("Response"))
//                        val httpBodyParser = coroutineScope.writer(autoFlush = true) {
//                            parseHttpBody(version, contentLength, transferEncoding, connectionType, input, channel)
//                        }
//                        httpBodyParser.channel
//                    }
//                }
                val body = myParserHttpBody(input)

                // FIXME
                //  val responseBody: Any = request.attributes.getOrNull(ResponseAdapterAttributeKey)
                //      ?.adapt(request, status, headers, body, request.body, callContext)
                //      ?: body
                val responseBody = body

                HttpResponseData(status, requestTime, headers, version, responseBody, callContext)
            }
        }

        else -> error("Unexpected ${HttpHeaders.ContentType}: $contentType")
    }
}

suspend fun myParserHttpBody(input: ByteReadChannel): String {
    return input.readRemaining().readText()
}