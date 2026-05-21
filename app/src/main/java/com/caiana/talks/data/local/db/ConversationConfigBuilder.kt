package com.caiana.talks.data.local.db

import com.caiana.talks.domain.model.AiPersona
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.ParticipantInfo
import com.caiana.talks.domain.model.SessionMode
import com.caiana.talks.domain.model.VoicePreference

fun UserProfileEntity.toConversationConfig(): ConversationConfig {
    val gender = toVoiceGender()
    val accent = toVoiceAccent()
    val rate = toSpeechRate()
    return ConversationConfig(
        mode = SessionMode.SINGLE,
        participants = listOf(ParticipantInfo(id, name)),
        learningGoal = toLearningGoal(),
        themes = toSelectedThemes(),
        voice = VoicePreference(gender, accent, rate),
        persona = AiPersona.of(gender, accent),
        cefrHint = null
    )
}

fun buildDualConfig(
    first: UserProfileEntity,
    second: UserProfileEntity
): ConversationConfig {
    val gender = first.toVoiceGender()
    val accent = first.toVoiceAccent()
    val rate = first.toSpeechRate()
    return ConversationConfig(
        mode = SessionMode.DUAL,
        participants = listOf(
            ParticipantInfo(first.id, first.name),
            ParticipantInfo(second.id, second.name)
        ),
        learningGoal = first.toLearningGoal(),
        themes = first.toSelectedThemes(),
        voice = VoicePreference(gender, accent, rate),
        persona = AiPersona.of(gender, accent),
        cefrHint = null
    )
}
