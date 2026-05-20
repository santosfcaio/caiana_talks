package com.caiana.talks.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE user_profile_id = :profileId ORDER BY started_at DESC")
    fun getSessionsForProfile(profileId: Int): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM sessions WHERE user_profile_id = :profileId")
    fun getSessionCount(profileId: Int): Flow<Int>
}
