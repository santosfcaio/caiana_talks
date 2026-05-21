package com.caiana.talks.domain.model

data class SessionResult(
    val outcome: Outcome,
    val summaries: List<ConversationSessionSummary>
) {
    enum class Outcome { SAVED, DISCARDED_TOO_SHORT }
}

data class ConversationSessionSummary(
    val sessionId: Int,
    val profileId: Int,
    val profileName: String,
    val startedAt: Long,
    val endedAt: Long,
    val correctionCount: Int,
    val vocabulary: List<String>,
    val coPracticeGroupId: String?
)
