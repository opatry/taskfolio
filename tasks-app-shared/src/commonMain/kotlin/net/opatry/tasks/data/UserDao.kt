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

package net.opatry.tasks.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import net.opatry.tasks.data.entity.UserEntity


@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM user WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): UserEntity?

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM user WHERE is_signed_in = true OR remote_id IS NULL LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("UPDATE user SET is_signed_in = false")
    suspend fun clearAllSignedInStatus()

    @Query("UPDATE user SET is_signed_in = false WHERE id = :id")
    suspend fun clearSignedInStatus(id: Long)

    @Transaction
    suspend fun setSignedInUser(userEntity: UserEntity) {
        clearAllSignedInStatus()
        insert(userEntity.copy(isSignedIn = true))
    }
}
