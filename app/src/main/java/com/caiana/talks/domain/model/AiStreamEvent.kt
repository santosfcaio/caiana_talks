package com.caiana.talks.domain.model

sealed interface AiStreamEvent {
    data class TextDelta(val text: String) : AiStreamEvent
    data object SayEnded : AiStreamEvent
    data class Completed(val meta: AiResponseMeta) : AiStreamEvent
    data class Failed(val error: ConversationError) : AiStreamEvent
}
