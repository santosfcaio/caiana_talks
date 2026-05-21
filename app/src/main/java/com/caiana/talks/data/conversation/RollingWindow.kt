package com.caiana.talks.data.conversation

import com.caiana.talks.domain.model.ConversationMessage

object RollingWindow {
    const val MAX_TURNS = 6

    fun take(allTurns: List<ConversationMessage>): List<ConversationMessage> =
        if (allTurns.size <= MAX_TURNS) allTurns else allTurns.takeLast(MAX_TURNS)
}
