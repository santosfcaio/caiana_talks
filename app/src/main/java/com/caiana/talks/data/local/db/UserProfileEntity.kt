package com.caiana.talks.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "learning_goals") val learningGoals: String = "",
    @ColumnInfo(name = "preferred_themes") val preferredThemes: String = "",
    @ColumnInfo(name = "ai_voice_gender") val aiVoiceGender: String = "feminine",
    @ColumnInfo(name = "ai_voice_accent") val aiVoiceAccent: String = "american",
    @ColumnInfo(name = "ai_speech_rate") val aiSpeechRate: String = "normal"
)
