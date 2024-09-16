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

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.keep.KeepService
import net.opatry.google.keep.entity.Note
import net.opatry.google.keep.entity.Section
import net.opatry.google.keep.entity.TextContent
import net.opatry.google.servicediscovery.GoogleServiceDiscovery

fun main() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    val serviceDiscovery = GoogleServiceDiscovery(httpClient)
    runBlocking {
        val keepServices = serviceDiscovery.list(name = "keep", preferred = true)
        val keepService = keepServices.items.firstOrNull()
        if (keepService != null) {
            println("Keep service: ${keepService.name} ${keepService.version} (id=${keepService.id}): ${keepService.discoveryRestUrl}")
        } else {
            error("No Keep service found")
        }

        val keepHttpClient = buildGoogleHttpClient(
            "https://keep.googleapis.com",
            "client_secret_1018227543555-d3olte26vocln883h887a9s6uv3cdveu.apps.googleusercontent.com.json",
            listOf(GoogleAuthenticator.Permission.Keep)
        ) {
            println("Please open the following URL in your browser to authenticate:")
            println(it)
        }
        val keepApi = KeepService(keepHttpClient)
        val note = keepApi.create(
            Note(
                name = "test",
                title = "Test note",
                body = Section(
                    text = TextContent("Hello, world!")
                )
            )
        )
        println("note=${note}")
        val note2 = keepApi.get(note.name)
        if (note != note2) {
            println("EERRR")
        } else {
            println("YATAAA!")
        }

        val notesResult = keepApi.list(10, "")
        notesResult.notes.forEach(::println)
        keepApi.delete(note.name)
        val notesResult2 = keepApi.list(10, "")
        notesResult2.notes.forEach(::println)
    }
}
