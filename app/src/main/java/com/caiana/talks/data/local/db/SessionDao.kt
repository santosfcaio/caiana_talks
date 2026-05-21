package com.caiana.talks.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE user_profile_id = :profileId ORDER BY started_at DESC")
    fun getSessionsForProfile(profileId: Int): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM sessions WHERE user_profile_id = :profileId")
    fun getSessionCount(profileId: Int): Flow<Int>

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'active'")
    suspend fun getActiveSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE co_practice_group_id = :groupId")
    suspend fun getSessionsByGroup(groupId: String): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)
}
