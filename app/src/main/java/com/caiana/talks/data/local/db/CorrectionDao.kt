package com.caiana.talks.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CategoryCount(val category: String, val count: Int)

@Dao
interface CorrectionDao {
    @Query("SELECT * FROM corrections WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getCorrectionsForSession(sessionId: Int): Flow<List<CorrectionEntity>>

    @Query("""
        SELECT c.category AS category, COUNT(*) AS count
        FROM corrections c
        INNER JOIN sessions s ON c.sessionId = s.id
        WHERE s.user_profile_id = :profileId
        GROUP BY c.category
    """)
    fun getCategoryCountsForProfile(profileId: Int): Flow<List<CategoryCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CorrectionEntity)
}
