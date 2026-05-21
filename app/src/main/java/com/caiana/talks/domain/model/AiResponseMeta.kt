package com.caiana.talks.domain.model

data class AiResponseMeta(
    val corrections: List<DetectedCorrection>,
    val vocabulary: List<String>,
    val userSpokePortuguese: Boolean
)

data class DetectedCorrection(
    val category: CorrectionCategory,
    val note: String
)
