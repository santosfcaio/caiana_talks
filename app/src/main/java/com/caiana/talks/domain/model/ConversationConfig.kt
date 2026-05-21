package com.caiana.talks.domain.model

data class ConversationConfig(
    val mode: SessionMode,
    val participants: List<ParticipantInfo>,
    val learningGoal: LearningGoal?,
    val themes: Set<ConversationTheme>,
    val voice: VoicePreference,
    val persona: AiPersona,
    val cefrHint: CefrLevel?
)

data class ParticipantInfo(
    val profileId: Int,
    val name: String
)
