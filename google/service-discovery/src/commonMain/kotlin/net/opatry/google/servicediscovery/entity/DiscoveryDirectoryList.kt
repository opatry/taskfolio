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

package net.opatry.google.servicediscovery.entity


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryDirectoryList(
    @SerialName("kind")
    val kind: String,
    @SerialName("discoveryVersion")
    val discoveryVersion: String,
    @SerialName("items")
    val items: List<Item> = emptyList()
) {
    @Serializable
    data class Item(
        @SerialName("kind")
        val kind: String,
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @SerialName("version")
        val version: String,
        @SerialName("title")
        val title: String = "",
        @SerialName("description")
        val description: String = "",
        @SerialName("discoveryRestUrl")
        val discoveryRestUrl: String,
        @SerialName("icons")
        val icons: Map<String, String> = emptyMap(),
        @SerialName("documentationLink")
        val documentationLink: String? = null,
        @SerialName("preferred")
        val preferred: Boolean = false,
        @SerialName("discoveryLink")
        val discoveryLink: String? = null,
    )
}