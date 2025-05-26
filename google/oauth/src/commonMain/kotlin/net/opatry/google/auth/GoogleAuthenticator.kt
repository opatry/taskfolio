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

package net.opatry.google.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.opatry.google.auth.GoogleAuthenticator.OAuthToken.TokenType.Bearer

interface GoogleAuthenticator {
    @JvmInline
    value class Scope(val value: String) {
        companion object {
            val Profile = Scope("https://www.googleapis.com/auth/userinfo.profile")
            val Email = Scope("https://www.googleapis.com/auth/userinfo.email")
            val OpenID = Scope("openid")
        }
    }

    /**
     * @property accessToken The token that your application sends to authorize a Google API request.
     * @property expiresIn The remaining lifetime of the access token in seconds.
     * @property idToken **Note:** This property is only returned if your request included an identity scope, such as  `openid`, `profile`, or `email`. The value is a JSON Web Token (JWT) that contains digitally signed identity information about the user.
     * @property refreshToken A token that you can use to obtain a new access token. Refresh tokens are valid until the user revokes access. Note that refresh tokens are always returned for installed applications.
     * @property scope The scopes of access granted by the [accessToken] expressed as a list of [Scope].
     * @property tokenType The type of token returned. At this time, this field's value is always set to `Bearer`.
     */
    @Serializable
    data class OAuthToken(
        @SerialName("access_token")
        val accessToken: String,

        @SerialName("expires_in")
        val expiresIn: Long,

        @SerialName("id_token")
        val idToken: String? = null,

        @SerialName("refresh_token")
        val refreshToken: String? = null,

        @SerialName("scope")
        val scope: String,

        @SerialName("token_type")
        val tokenType: TokenType,
    ) {
        /**
         * Value is case insensitive.
         *
         * @property Bearer `"Bearer"` token type defined in [RFC6750](https://datatracker.ietf.org/doc/html/rfc6750) is utilized by simply including the access token string in the request.
         */
        @Serializable
        enum class TokenType {
            @SerialName("Bearer")
            Bearer,
        }
    }

    sealed interface Grant {
        val type: String

        data class AuthorizationCode(val code: String) : Grant {
            override val type: String = "authorization_code"
        }

        data class RefreshToken(val refreshToken: String) : Grant {
            override val type: String
                get() = "refresh_token"
        }
    }

    /**
     * @param scopes Scopes to request to the user.
     * @param force To force user consent screen again (allowing to get a refresh token).
     * @param requestUserAuthorization The data needed to request user authorization before redirection
     *
     * @return auth code
     *
     * @see Scope
     */
    suspend fun authorize(scopes: List<Scope>, force: Boolean = false, requestUserAuthorization: (data: Any) -> Unit): String

    /**
     * @param code The code obtained through [authorize].
     *
     * @return OAuth access token
     *
     * @see Scope
     */
    suspend fun getToken(grant: Grant): OAuthToken
}
