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

package net.opatry.tasks.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.profile.UserInfoApi
import net.opatry.google.profile.model.UserInfo
import net.opatry.logging.Logger
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.NowProvider
import net.opatry.tasks.TokenCache
import net.opatry.tasks.data.UserDao
import net.opatry.tasks.data.entity.UserEntity
import kotlin.time.Duration.Companion.seconds


sealed interface UserState {
    data class SignedIn(
        val name: String,
        val email: String? = null,
        val avatarUrl: String? = null,
    ) : UserState

    data object Newcomer : UserState
    data object Unsigned : UserState
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
    private val logger: Logger,
    private val userDao: UserDao,
    private val credentialsStorage: CredentialsStorage,
    private val userInfoApi: UserInfoApi,
    private val nowProvider: NowProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<UserState?>(null)
    val state = _state.asStateFlow()

    private suspend fun fetchUserInfo(): UserInfo? {
        return try {
            userInfoApi.getUserInfo()
        } catch (e: ResponseException) {
            // don't assume we can read response accurately (see https://github.com/opatry/taskfolio/issues/262)
            // API is poorly documented and 401 & 400 do not return the same data for sure
            logger.logError("Web Service error while fetching user info", e)
            null
        } catch (e: Exception) {
            // most likely no network
            logger.logError("Error while fetching user info", e)
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
        val t0 = nowProvider.now()
        viewModelScope.launch {
            credentialsStorage.store(
                TokenCache(
                    token.accessToken,
                    token.refreshToken,
                    (t0 + token.expiresIn.seconds).toEpochMilliseconds()
                )
            )

            _state.value = fetchUserInfo()?.let { userInfo ->
                val userEntity = userInfo.asUserEntity(isSignedIn = true)
                userDao.setSignedInUser(userEntity)
                UserState.SignedIn(userEntity.name, userEntity.email, userEntity.avatarUrl)
            } ?: UserState.Unsigned
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