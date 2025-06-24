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

package net.opatry.tasks.presentation

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import net.opatry.google.auth.GoogleAuthenticator
import net.opatry.google.profile.UserInfoApi
import net.opatry.google.profile.model.UserInfo
import net.opatry.logging.Logger
import net.opatry.tasks.CredentialsStorage
import net.opatry.tasks.NowProvider
import net.opatry.tasks.TokenCache
import net.opatry.tasks.app.presentation.UserState
import net.opatry.tasks.app.presentation.UserViewModel
import net.opatry.tasks.data.UserDao
import net.opatry.tasks.data.entity.UserEntity
import net.opatry.test.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class UserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var userDao: UserDao

    @Mock
    private lateinit var credentialsStorage: CredentialsStorage

    @Mock
    private lateinit var userInfoApi: UserInfoApi

    @Mock
    private lateinit var nowProvider: NowProvider

    @Mock
    private lateinit var logger: Logger

    @InjectMocks
    private lateinit var viewModel: UserViewModel

    @Test
    fun `skipSignIn updates state to Unsigned and inserts unsigned user`() = runTest {
        viewModel.skipSignIn()
        advanceUntilIdle()

        then(userDao).should().insert(
            UserEntity(
                remoteId = null,
                name = "",
                isSignedIn = false,
            )
        )
        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `signIn stores token and updates state on successful user info fetch`() = runTest {
        given(nowProvider.now())
            .willReturn(Instant.fromEpochMilliseconds(42L))
        given(userInfoApi.getUserInfo()).willReturn(
            UserInfo(
                id = "remoteId",
                name = "name",
                email = "email",
                picture = "avatarUrl",
            )
        )

        viewModel.signIn(
            GoogleAuthenticator.OAuthToken(
                accessToken = "accessToken",
                expiresIn = 10L,
                idToken = "idToken",
                refreshToken = "refreshToken",
                scope = "scope",
                tokenType = GoogleAuthenticator.OAuthToken.TokenType.Bearer,
            )
        )
        advanceUntilIdle()

        then(credentialsStorage).should().store(
            TokenCache(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expirationTimeMillis = 42L + 10L.seconds.inWholeMilliseconds,
            )
        )
        then(userDao).should().setSignedInUser(
            UserEntity(
                remoteId = "remoteId",
                name = "name",
                email = "email",
                avatarUrl = "avatarUrl",
                isSignedIn = true,
            )
        )
        assertEquals(
            UserState.SignedIn(
                name = "name",
                email = "email",
                avatarUrl = "avatarUrl",
            ),
            viewModel.state.value
        )
    }

    @Test
    fun `signIn stores token but updates state to Unsigned on failed user info fetch`() = runTest {
        given(nowProvider.now())
            .willReturn(Instant.fromEpochMilliseconds(42L))
        given(userInfoApi.getUserInfo()).willThrow(RuntimeException("Network error"))

        viewModel.signIn(
            GoogleAuthenticator.OAuthToken(
                accessToken = "accessToken",
                expiresIn = 10L,
                idToken = "idToken",
                refreshToken = "refreshToken",
                scope = "scope",
                tokenType = GoogleAuthenticator.OAuthToken.TokenType.Bearer,
            )
        )
        advanceUntilIdle()

        then(credentialsStorage).should().store(
            TokenCache(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expirationTimeMillis = 42L + 10L.seconds.inWholeMilliseconds,
            )
        )
        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `signIn stores token but updates state to Unsigned on WS failure when fetching user info`() = runTest {
        given(nowProvider.now())
            .willReturn(Instant.fromEpochMilliseconds(42L))
        val exception = mock<ResponseException>()
        given(userInfoApi.getUserInfo()).willThrow(exception)

        viewModel.signIn(
            GoogleAuthenticator.OAuthToken(
                accessToken = "accessToken",
                expiresIn = 10L,
                idToken = "idToken",
                refreshToken = "refreshToken",
                scope = "scope",
                tokenType = GoogleAuthenticator.OAuthToken.TokenType.Bearer,
            )
        )
        advanceUntilIdle()

        verify(credentialsStorage).store(
            TokenCache(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expirationTimeMillis = 42L + 10L.seconds.inWholeMilliseconds,
            )
        )
        verify(logger).logError("Web Service error while fetching user info", exception)
        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `signIn stores token but updates state to Unsigned on unknown failure when fetching user info`() = runTest {
        given(nowProvider.now())
            .willReturn(Instant.fromEpochMilliseconds(42L))
        val exception = mock<RuntimeException>()
        given(userInfoApi.getUserInfo()).willThrow(exception)

        viewModel.signIn(
            GoogleAuthenticator.OAuthToken(
                accessToken = "accessToken",
                expiresIn = 10L,
                idToken = "idToken",
                refreshToken = "refreshToken",
                scope = "scope",
                tokenType = GoogleAuthenticator.OAuthToken.TokenType.Bearer,
            )
        )
        advanceUntilIdle()

        verify(credentialsStorage).store(
            TokenCache(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
                expirationTimeMillis = 42L + 10L.seconds.inWholeMilliseconds,
            )
        )
        verify(logger).logError("Error while fetching user info", exception)
        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `signOut clears token clears signed in status and updates state to Unsigned`() = runTest {
        viewModel.signOut()
        advanceUntilIdle()

        verify(credentialsStorage).store(TokenCache())
        verify(userDao).clearAllSignedInStatus()
        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `refreshUserState updates state to Newcomer when no current user exists`() = runTest {
        given(userDao.getCurrentUser()).willReturn(null)

        viewModel.refreshUserState()
        advanceUntilIdle()

        assertEquals(UserState.Newcomer, viewModel.state.value)
    }

    @Test
    fun `refreshUserState updates state to Unsigned when current user has no remote ID`() = runTest {
        given(userDao.getCurrentUser()).willReturn(
            UserEntity(
                remoteId = null,
                name = "name",
            )
        )

        viewModel.refreshUserState()
        advanceUntilIdle()

        assertEquals(UserState.Unsigned, viewModel.state.value)
    }

    @Test
    fun `refreshUserState updates state to SignedIn when current user has a remote ID`() = runTest {
        given(userDao.getCurrentUser()).willReturn(
            UserEntity(
                remoteId = "remoteId",
                name = "name",
            )
        )

        viewModel.refreshUserState()
        advanceUntilIdle()

        assertEquals(UserState.SignedIn("name"), viewModel.state.value)
    }
}