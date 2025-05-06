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

package net.opatry.google.profile

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import net.opatry.google.profile.model.UserInfo
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun MockRequestHandleScope.respondJson(json: String): HttpResponseData =
    respond(
        content = json,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

private suspend fun usingUserInfoApi(
    response: MockRequestHandleScope.() -> HttpResponseData,
    test: suspend (HttpUserInfoApi) -> Unit,
) {
    MockEngine {
        response()
    }.use { mockEngine ->
        HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }.use { httpClient ->
            test(HttpUserInfoApi(httpClient))
        }
    }
}

class HttpUserInfoApiTest {

    @Test
    fun `Successful user info retrieval`() = runTest {
        usingUserInfoApi(
            response = {
                respondJson(
                    """{
                        "id": "id",
                        "name": "name",
                        "email": "email",
                        "picture": "picture"
                    }""".trimIndent()
                )
            }
        ) { userInfoApi ->
            val userInfo = userInfoApi.getUserInfo()

            assertEquals(
                UserInfo(
                    id = "id",
                    name = "name",
                    email = "email",
                    picture = "picture"
                ),
                userInfo
            )
        }
    }

    @Test
    fun `Failed user info retrieval`() = runTest {
        usingUserInfoApi(
            response = {
                respondBadRequest()
            }
        ) { userInfoApi ->
            assertFailsWith<ClientRequestException> {
                userInfoApi.getUserInfo()
            }
        }
    }
}