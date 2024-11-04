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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.opatry.google.tasks.batch.Response.Status.Code.ALREADY_EXISTS
import net.opatry.google.tasks.batch.Response.Status.Code.FAILED_PRECONDITION
import net.opatry.google.tasks.batch.Response.Status.Code.NOT_FOUND
import net.opatry.google.tasks.batch.Response.Status.Code.OUT_OF_RANGE

/**
 * A [response](https://developers.google.com/tasks/reference/rest/v1/Response) to a single Request.
 *
 * Either there is an [error] or a [response], not both or none, see [isError] & [isSuccess].
 *
 * @property requestId The requestId of the [RequestBatch.Request] this message is in response to.
 * @property isContinued If `true`, this [Response] is followed by additional responses that are in the same response stream as this [Response].
 * @property extensions Application specific response metadata.
 *                      An object containing fields of an arbitrary type. An additional field `"@type"` contains a URI identifying the type. Example: `{ "id": 1234, "@type": "types.example.com/standard/id" }`.
 * @property error The error result if there was an error processing the request.
 * @property response The response payload if the call was a success.
 *                    An object containing fields of an arbitrary type. An additional field `"@type"` contains a URI identifying the type. Example: `{ "id": 1234, "@type": "types.example.com/standard/id" }`.
 */
@Serializable
data class Response(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("continued")
    val isContinued: Boolean,
    @SerialName("extensions")
    val extensions: List<Map<String, String/*FIXME Any serializable type*/>>,
    @SerialName("error")
    val error: Status? = null,
    @SerialName("response")
    val response: Map<String, String/*FIXME Any serializable type*/>? = null
) {
    val isError
        get() = error != null
    val isSuccess
        get() = response != null

    init {
        require(isError xor isSuccess) { "Either error or response must be set, but not both or none" }
    }

    /**
     * The [Status] type defines a logical error model that is suitable for different programming environments, including REST APIs and RPC APIs. It is used by [gRPC](https://github.com/grpc). Each [Status] message contains three pieces of data: error code, error message, and error details.
     * You can find out more about this error model and how to work with it in the [API Design Guide](https://cloud.google.com/apis/design/errors).
     *
     * @property code The status code
     * @property message A developer-facing error message, which should be in English. Any user-facing error message should be localized and sent in the [details] field, or localized by the client.
     * @property details A list of messages that carry the error details. There is a common set of message types for APIs to use.
     *                   An object containing fields of an arbitrary type. An additional field `"@type"` contains a URI identifying the type. Example: `{ "id": 1234, "@type": "types.example.com/standard/id" }`.
     */
    @Serializable
    data class Status(
        @SerialName("code")
        val code: Code,
        @SerialName("message")
        val message: String,
        @SerialName("details")
        val details: List<Map<String, String/*FIXME Any serializable type*/>>,
    ) {
        private object CodeSerializer : KSerializer<Code> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Code", PrimitiveKind.INT)

            override fun serialize(encoder: Encoder, value: Code) {
                encoder.encodeInt(value.value)
            }

            override fun deserialize(decoder: Decoder): Code {
                val v = decoder.decodeInt()
                return Code.entries.first { it.value == v }
            }
        }

        /**
         * The canonical [error codes for gRPC APIs](https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto).
         * Sometimes multiple error codes may apply.
         * Services should return the most specific error code that applies.
         * For example, prefer [OUT_OF_RANGE] over [FAILED_PRECONDITION] if both codes apply.
         * Similarly prefer [NOT_FOUND] or [ALREADY_EXISTS] over [FAILED_PRECONDITION].
         */
        @Serializable(with = CodeSerializer::class)
        enum class Code(val value: Int, val httpStatusCode: Int) {
            /**
             * Not an error; returned on success.
             *
             * HTTP Mapping: 200 OK
             */
            OK(0, 200),

            /**
             * The operation was cancelled, typically by the caller.
             *
             * HTTP Mapping: 499 Client Closed Request
             */
            CANCELLED(1, 499),

            /**
             * Unknown error.
             * For example, this error may be returned when a [Status] value received from another address space belongs to an error space that is not known in this address space.
             * Also errors raised by APIs that do not return enough error information may be converted to this error.
             *
             * HTTP Mapping: 500 Internal Server Error
             */
            UNKNOWN(2, 500),

            /**
             * The client specified an invalid argument.
             * Note that this differs from [FAILED_PRECONDITION].
             * [INVALID_ARGUMENT] indicates arguments that are problematic regardless of the state of the system (e.g., a malformed file name).
             *
             * HTTP Mapping: 400 Bad Request
             */
            INVALID_ARGUMENT(3, 400),

            /**
             * The deadline expired before the operation could complete. For operations that change the state of the system, this error may be returned even if the operation has completed successfully.
             * For example, a successful response from a server could have been delayed long enough for the deadline to expire.
             *
             * HTTP Mapping: 504 Gateway Timeout
             */
            DEADLINE_EXCEEDED(4, 504),

            /**
             * Some requested entity (e.g., file or directory) was not found.
             *
             * Note to server developers: if a request is denied for an entire class of users, such as gradual feature rollout or undocumented allowlist, [NOT_FOUND] may be used. If a request is denied for some users within a class of users, such as user-based access control, [PERMISSION_DENIED] must be used.
             *
             * HTTP Mapping: 404 Not Found
             */
            NOT_FOUND(5, 404),

            /**
             * The entity that a client attempted to create (e.g., file or directory) already exists.
             *
             * HTTP Mapping: 409 Conflict
             */
            ALREADY_EXISTS(6, 409),

            /**
             * The caller does not have permission to execute the specified operation.
             * [PERMISSION_DENIED] must not be used for rejections caused by exhausting some resource (use [RESOURCE_EXHAUSTED] instead for those errors). [PERMISSION_DENIED] must not be used if the caller can not be identified (use [UNAUTHENTICATED] instead for those errors). This error code does not imply the request is valid or the requested entity exists or satisfies other pre-conditions.
             *
             * HTTP Mapping: 403 Forbidden
             */
            PERMISSION_DENIED(7, 403),

            /**
             * The request does not have valid authentication credentials for the * operation.
             *
             * HTTP Mapping: 401 Unauthorized
             */
            UNAUTHENTICATED(16, 401),

            /**
             * Some resource has been exhausted, perhaps a per-user quota, or perhaps the entire file system is out of space.
             *
             * HTTP Mapping: 429 Too Many Requests
             */
            RESOURCE_EXHAUSTED(8, 429),

            /**
             * The operation was rejected because the system is not in a state required for the operation's execution.  For example, the directory to be deleted is non-empty, an rmdir operation is applied to a non-directory, etc.
             *
             * Service implementors can use the following guidelines to decide between [FAILED_PRECONDITION], [ABORTED], and [UNAVAILABLE]:
             *  (a) Use [UNAVAILABLE] if the client can retry just the failing call.
             *  (b) Use [ABORTED] if the client should retry at a higher level. For
             *      example, when a client-specified test-and-set fails, indicating the
             *      client should restart a read-modify-write sequence.
             *  (c) Use [FAILED_PRECONDITION] if the client should not retry until
             *      the system state has been explicitly fixed. For example, if an "rmdir"
             *      fails because the directory is non-empty, [FAILED_PRECONDITION]
             *      should be returned since the client should not retry unless
             *      the files are deleted from the directory.
             *
             * HTTP Mapping: 400 Bad Request
             */
            FAILED_PRECONDITION(9, 400),

            /**
             * The operation was aborted, typically due to a concurrency issue such as a sequencer check failure or transaction abort.
             *
             * See the guidelines above for deciding between [FAILED_PRECONDITION], [ABORTED], and [UNAVAILABLE].
             *
             * HTTP Mapping: 409 Conflict
             */
            ABORTED(10, 409),

            /**
             * The operation was attempted past the valid range.
             * E.g., seeking or reading past end-of-file.
             *
             * Unlike [INVALID_ARGUMENT], this error indicates a problem that may be fixed if the system state changes. For example, a 32-bit file system will generate [INVALID_ARGUMENT] if asked to read at an offset that is not in the range `[0,2^32-1]`, but it will generate [OUT_OF_RANGE] if asked to read from an offset past the current file size.
             *
             * There is a fair bit of overlap between [FAILED_PRECONDITION] and [OUT_OF_RANGE].  We recommend using [OUT_OF_RANGE] (the more specific error) when it applies so that callers who are iterating through a space can easily look for an [OUT_OF_RANGE] error to detect when they are done.
             *
             * HTTP Mapping: 400 Bad Request
             */
            OUT_OF_RANGE(11, 400),

            /**
             * The operation is not implemented or is not supported/enabled in this service.
             *
             * HTTP Mapping: 501 Not Implemented
             */
            UNIMPLEMENTED(12, 501),

            /**
             * Internal errors.
             * This means that some invariants expected by the
             * underlying system have been broken.
             * This error code is reserved for serious errors.
             *
             * HTTP Mapping: 500 Internal Server Error
             */
            INTERNAL(13, 500),

            /**
             * The service is currently unavailable.
             * This is most likely a transient condition, which can be corrected by retrying with a backoff. Note that it is not always safe to retry non-idempotent operations.
             *
             * See the guidelines above for deciding between [FAILED_PRECONDITION], [ABORTED], and [UNAVAILABLE].
             *
             * HTTP Mapping: 503 Service Unavailable
             */
            UNAVAILABLE(14, 503),

            /**
             * Unrecoverable data loss or corruption.
             *
             * HTTP Mapping: 500 Internal Server Error
             */
            DATA_LOSS(15, 500),
        }
    }
}
