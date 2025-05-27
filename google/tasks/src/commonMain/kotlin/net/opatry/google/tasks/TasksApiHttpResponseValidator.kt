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

package net.opatry.google.tasks

import io.ktor.client.plugins.HttpCallValidatorConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import net.opatry.google.tasks.model.ErrorResponse

class TasksApiHttpResponseValidator(private val host: String) : (HttpCallValidatorConfig) -> Unit {
    override fun invoke(config: HttpCallValidatorConfig) {
        config.handleResponseExceptionWithRequest { exception, request ->
            when {
                request.url.host == host && exception is ResponseException -> {
                    // can't rely on default ktor deserialization for error responses
                    // the ContentEncoding plugin is short-circuited for error responses
                    // need to manually decode the error response body
                    val errorBody = exception.response.bodyAsText()
                    val error = Json.decodeFromString<ErrorResponse>(errorBody)
                    throw TasksApiException(error)
                }

                else -> throw exception
            }
        }
    }
}