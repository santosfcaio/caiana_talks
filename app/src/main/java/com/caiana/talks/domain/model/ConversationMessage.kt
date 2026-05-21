package com.caiana.talks.domain.model

data class ConversationMessage(
    val role: Role,
    val text: String
) {
    enum class Role { USER, ASSISTANT }
}
