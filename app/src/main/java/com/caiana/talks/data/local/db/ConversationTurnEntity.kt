package com.caiana.talks.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_turns",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "session_id") val sessionId: Int,
    @ColumnInfo(name = "speaker_profile_id") val speakerProfileId: Int,
    @ColumnInfo(name = "turn_index") val turnIndex: Int,
    @ColumnInfo(name = "user_text") val userText: String,
    @ColumnInfo(name = "ai_text") val aiText: String = "",
    val timestamp: Long
)
