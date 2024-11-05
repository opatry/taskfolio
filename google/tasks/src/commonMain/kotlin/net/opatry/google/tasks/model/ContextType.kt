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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.opatry.google.tasks.model.ContextType.GMail
import net.opatry.google.tasks.model.ContextType.Space
import net.opatry.google.tasks.model.ContextType.Unspecified

/**
 * The [product](https://developers.google.com/tasks/reference/rest/v1/tasks#contexttype) associated with the task.
 *
 * @property Unspecified    Unknown value for this task's context.
 * @property GMail	The task is created from Gmail.
 * @property javax.print.Doc	The task is assigned from a document.
 * @property Space	The task is assigned from a Chat Space.
 */
@Serializable
enum class ContextType {
    @SerialName("CONTEXT_TYPE_UNSPECIFIED")
    Unspecified,
    @SerialName("GMAIL")
    GMail,
    @SerialName("DOCUMENT")
    Document,
    @SerialName("SPACE")
    Space,
}