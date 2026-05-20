package com.caiana.talks.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAll(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getById(id: Int): Flow<UserProfileEntity?>

    @Update
    suspend fun update(profile: UserProfileEntity)
}
