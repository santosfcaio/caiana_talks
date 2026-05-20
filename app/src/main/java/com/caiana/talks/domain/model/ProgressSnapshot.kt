package com.caiana.talks.domain.model

data class ProgressSnapshot(
    val cefrLevel: CefrLevel?,
    val grammarErrors: Int,
    val vocabularyErrors: Int,
    val fluencyErrors: Int,
    val sessions: List<SessionSummary>,
    val insights: List<String>
)

data class SessionSummary(
    val id: Int,
    val date: Long,
    val durationMinutes: Int,
    val corrections: List<CorrectionSummary>
)

data class CorrectionSummary(
    val category: CorrectionCategory,
    val description: String
)
