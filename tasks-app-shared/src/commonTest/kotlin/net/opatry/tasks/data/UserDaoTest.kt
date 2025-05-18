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

package net.opatry.tasks.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.opatry.tasks.data.entity.UserEntity
import net.opatry.tasks.data.util.inMemoryTasksAppDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


private fun runWithInMemoryDatabase(
    test: suspend TestScope.(UserDao) -> Unit
) = runTest {
    val db = inMemoryTasksAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(backgroundScope.coroutineContext)
        .build()

    try {
        test(db.getUserDao())
    } finally {
        db.close()
    }
}

class UserDaoTest {

    @Test
    fun `when insert full user then it should be inserted and returned by getById with same content`() = runWithInMemoryDatabase { userDao ->
        val user = UserEntity(
            remoteId = "remoteId",
            name = "name",
            email = "email",
            avatarUrl = "avatarUrl",
            isSignedIn = true,
        )

        val id = userDao.insert(user)

        val insertedUser = userDao.getById(id)
        assertNotNull(insertedUser)
        assertEquals(id, insertedUser.id)
        assertEquals("remoteId", insertedUser.remoteId)
        assertEquals("name", insertedUser.name)
        assertEquals("email", insertedUser.email)
        assertEquals("avatarUrl", insertedUser.avatarUrl)
        assertTrue(insertedUser.isSignedIn)
    }

    @Test
    fun `when upsert user with remoteId then it should be inserted`() = runWithInMemoryDatabase { userDao ->        // Given
        val user = UserEntity(
            remoteId = "remoteId",
            name = "name",
            email = "email",
            avatarUrl = "avatarUrl",
            isSignedIn = true,
        )
        val id = userDao.insert(user)

        val updatedId = userDao.upsert(user.copy(id = id, remoteId = "newRemoteId", name = "newName"))

        val updatedUser = userDao.getById(id)
        assertNotNull(updatedUser)
        assertEquals(id, updatedId)
        assertEquals(updatedId, updatedUser.id)
        assertEquals("newRemoteId", updatedUser.remoteId)
        assertEquals("newName", updatedUser.name)
    }

    @Test
    fun `when insert remote user then getByRemoteId(remoteId) should return the remote user`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(remoteId = "remoteId", name = "name", isSignedIn = true))

        val remoteUser = userDao.getByRemoteId("remoteId")

        assertNotNull(remoteUser)
        assertEquals(userId, remoteUser.id)
        assertEquals("remoteId", remoteUser.remoteId)
        assertEquals("name", remoteUser.name)
    }

    @Test
    fun `when querying user by remote id with invalid id then should return null`() = runWithInMemoryDatabase { userDao ->
        userDao.insert(UserEntity(remoteId = "remoteId", name = "name"))

        val user = userDao.getByRemoteId("invalidRemoteId")

        assertNull(user)
    }

    @Test
    fun `when insert remote user then getById(Id) should return the user`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(name = "name", isSignedIn = false))

        val user = userDao.getById(userId)

        assertNotNull(user)
        assertEquals(userId, user.id)
        assertFalse(user.isSignedIn)
    }

    @Test
    fun `when querying user by id with invalid id then should return null`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(name = "name"))

        val user = userDao.getById(userId - 1)

        assertNull(user)
    }

    @Test
    fun `when clearAllSignedInStatus then all signed in users should be unsigned`() = runWithInMemoryDatabase { userDao ->
        val user0Id = userDao.insert(UserEntity(name = "name3", isSignedIn = false))
        val user1Id = userDao.insert(UserEntity(remoteId = "remoteId1", name = "name1", isSignedIn = true))
        val user2Id = userDao.insert(UserEntity(remoteId = "remoteId2", name = "name2", isSignedIn = true))

        userDao.clearAllSignedInStatus()

        val user0 = userDao.getById(user0Id)
        assertNotNull(user0)
        assertFalse(user0.isSignedIn)
        val user1 = userDao.getById(user1Id)
        assertNotNull(user1)
        assertFalse(user1.isSignedIn)
        val user2 = userDao.getById(user2Id)
        assertNotNull(user2)
        assertFalse(user2.isSignedIn)
    }

    @Test
    fun `when clearSignedInStatus(id) then signed in user with id should be unsigned`() = runWithInMemoryDatabase { userDao ->
        val user0Id = userDao.insert(UserEntity(name = "name3", isSignedIn = true))
        val user1Id = userDao.insert(UserEntity(remoteId = "remoteId1", name = "name1", isSignedIn = true))

        userDao.clearSignedInStatus(user1Id)

        val user0 = userDao.getById(user0Id)
        assertNotNull(user0)
        assertTrue(user0.isSignedIn)
        val user1 = userDao.getById(user1Id)
        assertNotNull(user1)
        assertFalse(user1.isSignedIn)
    }

    @Test
    fun `when getCurrentUser without signed in user then should return unsigned user`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(name = "name", isSignedIn = false))

        val user = userDao.getCurrentUser()

        assertNotNull(user)
        assertEquals(userId, user.id)
    }

    @Test
    fun `when getCurrentUser with signed in user then should return signed user`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(name = "name", isSignedIn = false))

        val user = userDao.getCurrentUser()

        assertNotNull(user)
        assertEquals(userId, user.id)
    }

    @Test
    fun `when getCurrentUser without user then should return null`() = runWithInMemoryDatabase { userDao ->
        val userId = userDao.insert(UserEntity(name = "name", isSignedIn = false))

        val user = userDao.getCurrentUser()

        assertNotNull(user)
        assertEquals(userId, user.id)
    }

    @Test
    fun `when setSignedInUser then any signed in user should be unsigned and given user signed`() = runWithInMemoryDatabase { userDao ->
        val user0Id = userDao.insert(UserEntity(name = "name3", isSignedIn = false))
        val user1Id = userDao.insert(UserEntity(remoteId = "remoteId1", name = "name1", isSignedIn = true))
        val user2Id = userDao.insert(UserEntity(remoteId = "remoteId1", name = "name1", isSignedIn = true))

        val user0 = userDao.getById(user0Id)
        assertNotNull(user0)
        userDao.setSignedInUser(user0)

        val signedUser0 = userDao.getById(user0Id)
        assertNotNull(signedUser0)
        assertTrue(signedUser0.isSignedIn)
        val user1 = userDao.getById(user1Id)
        assertNotNull(user1)
        assertFalse(user1.isSignedIn)
        val user2 = userDao.getById(user2Id)
        assertNotNull(user2)
        assertFalse(user2.isSignedIn)
    }
}