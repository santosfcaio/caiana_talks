package com.caiana.talks.data.remote

import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationMessage
import kotlinx.coroutines.flow.Flow

data class SystemPrompt(
    val staticBlock: String,
    val personalizationBlock: String
)

interface ConversationAiClient {
    fun streamReply(
        system: SystemPrompt,
        window: List<ConversationMessage>,
        userInput: String
    ): Flow<AiStreamEvent>
}
