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

package net.opatry.tasks.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.profile.model.UserInfo
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.TokenCache
import net.opatry.tasks.data.UserDao
import net.opatry.tasks.data.entity.UserEntity
import kotlin.time.Duration.Companion.seconds


sealed class UserState {
    data class SignedIn(
        val name: String,
        val email: String? = null,
        val avatarUrl: String? = null,
    ) : UserState()
    data object Newcomer : UserState()
    data object Unsigned : UserState()
}

private fun UserInfo.asUserEntity(isSignedIn: Boolean): UserEntity {
    return UserEntity(
        remoteId = id,
        name = name,
        email = email,
        avatarUrl = picture,
        isSignedIn = isSignedIn,
    )
}

class UserViewModel(
    private val userDao: UserDao,
    private val credentialsStorage: CredentialsStorage,
    private val httpClient: HttpClient,
) : ViewModel() {

    private val _state = MutableStateFlow<UserState?>(null)
    val state: Flow<UserState?>
        get() = _state

    private suspend fun fetchUserInfo(): UserInfo? {
        return try {
            val response = httpClient.get("https://www.googleapis.com/oauth2/v1/userinfo?alt=json")

            if (response.status.isSuccess()) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            // most likely no network
            null
        }
    }

    fun skipSignIn() {
        viewModelScope.launch {
            userDao.insert(UserEntity(remoteId = null, name = "", isSignedIn = false))
            _state.value = UserState.Unsigned
        }
    }

    fun signIn(token: GoogleAuthenticator.OAuthToken) {
        val t0 = Clock.System.now()
        viewModelScope.launch {
            credentialsStorage.store(
                TokenCache(
                    token.accessToken,
                    token.refreshToken,
                    (t0 + token.expiresIn.seconds).toEpochMilliseconds()
                )
            )

            val userInfo = fetchUserInfo()
            if (userInfo != null) {
                val userEntity = userInfo.asUserEntity(isSignedIn = true)
                userDao.setSignedInUser(userEntity)
                _state.value = UserState.SignedIn(userEntity.name, userEntity.email, userEntity.avatarUrl)
            } else {
                _state.value = UserState.Unsigned
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            credentialsStorage.store(TokenCache())

            userDao.clearAllSignedInStatus()
            _state.value = UserState.Unsigned
        }
    }

    fun refreshUserState() {
        viewModelScope.launch {
            val currentUser = userDao.getCurrentUser()
            _state.value = when {
                currentUser == null -> UserState.Newcomer
                currentUser.remoteId == null -> UserState.Unsigned
                else -> UserState.SignedIn(currentUser.name, currentUser.email, currentUser.avatarUrl)
            }
        }
    }
}