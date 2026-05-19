package com.caiana.talks.data.local.db

import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender

fun UserProfileEntity.toLearningGoal(): LearningGoal? =
    LearningGoal.entries.find { it.id == learningGoals }

fun UserProfileEntity.toSelectedThemes(): Set<ConversationTheme> {
    if (preferredThemes.isBlank()) return emptySet()
    return preferredThemes
        .split(",")
        .mapNotNull { token -> ConversationTheme.entries.find { it.id == token.trim() } }
        .toSet()
}

fun UserProfileEntity.toVoiceGender(): VoiceGender =
    VoiceGender.entries.find { it.id == aiVoiceGender } ?: VoiceGender.FEMININE

fun UserProfileEntity.toVoiceAccent(): VoiceAccent =
    VoiceAccent.entries.find { it.id == aiVoiceAccent } ?: VoiceAccent.AMERICAN

fun UserProfileEntity.toSpeechRate(): SpeechRate =
    SpeechRate.entries.find { it.id == aiSpeechRate } ?: SpeechRate.NORMAL
