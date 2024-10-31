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

package net.opatry.google.tasks.batch

import io.ktor.http.cio.Request
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A [batch of requests](https://developers.google.com/tasks/reference/rest/v1/RequestBatch) to Execute.
 *
 * @property name The name of the resource this request is for. Some Batch implementations may require a batch to be for only a single resource, for example a single database.
 * @property requests The requests contained in this batch.
 */
@Serializable
data class RequestBatch(
    @SerialName("name")
    val name: String,
    @SerialName("requests")
    val requests: List<Request>,
) {
    /**
     * A [request message](https://developers.google.com/tasks/reference/rest/v1/Request) sent as part of a batch execution.
     *
     * @property requestId Unique id of this request within the batch. The Response message with a matching `requestId` is the response to this request. For request-streaming methods, the same `requestId` may be used multiple times to pass all request messages that are part of a single method. For response-streaming methods, the same `requestId` may show up in multiple Response messages.
     * @property methodName The method being called. Must be a fully qualified method name. Example: `google.rpc.batch.Batch.Execute`
     * @property request The request payload.
     *                   An object containing fields of an arbitrary type. An additional field `"@type"` contains a URI identifying the type. Example: `{ "id": 1234, "@type": "types.example.com/standard/id" }`.
     * @property extensions Application specific request metadata.
     *                      An object containing fields of an arbitrary type. An additional field `"@type"` contains a URI identifying the type. Example: `{ "id": 1234, "@type": "types.example.com/standard/id" }`.
     */
    @Serializable
    data class Request(
        @SerialName("requestId")
        val requestId: String,
        @SerialName("methodName")
        val methodName: String,
        @SerialName("request")
        val request: Map<String, String/*FIXME Any serializable type*/>, // "@type" is a string, but the rest is dynamic
        @SerialName("extensions")
        val extensions: List<Map<String, String/*FIXME Any serializable type*/>>,
    )
}