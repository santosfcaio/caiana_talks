package com.caiana.talks.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationTurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ConversationTurnEntity): Long

    @Query("SELECT * FROM conversation_turns WHERE session_id = :sessionId ORDER BY turn_index ASC")
    suspend fun getTurnsForSession(sessionId: Int): List<ConversationTurnEntity>

    @Query("SELECT * FROM conversation_turns WHERE session_id = :sessionId ORDER BY turn_index ASC")
    fun observeTurnsForSession(sessionId: Int): Flow<List<ConversationTurnEntity>>
}
