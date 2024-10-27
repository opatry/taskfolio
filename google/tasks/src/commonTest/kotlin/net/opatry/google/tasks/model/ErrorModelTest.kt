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

package net.opatry.google.tasks.model

import kotlinx.coroutines.test.runTest
import net.opatry.google.tasks.util.loadJsonAsObject
import kotlin.test.Test
import kotlin.test.assertEquals


class ErrorModelTest {
    @Test
    fun `parse error response from json`() = runTest {
        val errorResponse = loadJsonAsObject<ErrorResponse>("/error_400.json")
        assertEquals(
            ErrorResponse(
                error = ErrorResponse.Error(
                    code = 400,
                    message = "Invalid task list ID",
                    errors = listOf(
                        ErrorResponse.Error.ErrorDetail(
                            message = "Invalid task list ID",
                            domain = "global",
                            reason = "invalid"
                        )
                    )
                )
            ),
            errorResponse
        )
    }
}