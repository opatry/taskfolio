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

package net.opatry.tasks.app.di

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.opatry.google.auth.GoogleAuth
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.auth.HttpGoogleAuthenticator
import net.opatry.tasks.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.dsl.module

@OptIn(ExperimentalResourceApi::class, ExperimentalSerializationApi::class)
val authModule = module {
    single<GoogleAuthenticator> {
        val credentialsFilename = "client_secret_1018227543555-k121h4da66i87lpione39a7et0lkifqi.apps.googleusercontent.com.json"
        val googleAuthCredentials = runBlocking {
            Res.readBytes("files/$credentialsFilename").inputStream().use { inputStream ->
                // expects a `web` credentials, if `installed` is needed, inject another way
                requireNotNull(Json.decodeFromStream<GoogleAuth>(inputStream).webCredentials)
            }
        }

        val config = HttpGoogleAuthenticator.ApplicationConfig(
            redirectUrl = googleAuthCredentials.redirectUris.first(),
            clientId = googleAuthCredentials.clientId,
            clientSecret = googleAuthCredentials.clientSecret,
            authUri = googleAuthCredentials.authUri,
            tokenUri = googleAuthCredentials.tokenUri,
        )
        HttpGoogleAuthenticator(config)
    }
}