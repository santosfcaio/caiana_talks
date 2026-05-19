package com.caiana.talks.domain.model

data class VoicePreference(
    val gender: VoiceGender,
    val accent: VoiceAccent,
    val rate: SpeechRate
)

data class ProfilePreferences(
    val learningGoal: LearningGoal?,
    val selectedThemes: Set<ConversationTheme>,
    val voicePreference: VoicePreference
)
