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

package net.opatry.google.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/**
 * Represents a Google OAuth2 client credentials (`client_secret_xxx.apps.googleusercontent.com.json` files).
 */
data class GoogleAuth(
    @SerialName("web")
    val webCredentials: Credentials.Web? = null,
    @SerialName("installed")
    val installedCredentials: Credentials.Installed? = null,
) {
    sealed class Credentials {
        abstract val clientId: String
        abstract val projectId: String
        abstract val authUri: String
        abstract val tokenUri: String
        abstract val authProviderX509CertUrl: String

        @Serializable
        data class Web(
            @SerialName("client_id")
            override val clientId: String,
            @SerialName("project_id")
            override val projectId: String,
            @SerialName("auth_uri")
            override val authUri: String,
            @SerialName("token_uri")
            override val tokenUri: String,
            @SerialName("auth_provider_x509_cert_url")
            override val authProviderX509CertUrl: String,
            @SerialName("client_secret")
            val clientSecret: String,
            @SerialName("redirect_uris")
            val redirectUris: List<String>,
        ) : Credentials()

        @Serializable
        data class Installed(
            @SerialName("client_id")
            override val clientId: String,
            @SerialName("project_id")
            override val projectId: String,
            @SerialName("auth_uri")
            override val authUri: String,
            @SerialName("token_uri")
            override val tokenUri: String,
            @SerialName("auth_provider_x509_cert_url")
            override val authProviderX509CertUrl: String,
        ) : Credentials()
    }
}
