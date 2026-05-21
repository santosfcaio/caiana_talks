package com.caiana.talks.data.conversation

import com.caiana.talks.data.remote.SystemPrompt
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.SessionMode
import javax.inject.Inject

interface SystemPromptBuilder {
    fun build(config: ConversationConfig): SystemPrompt
}

class SystemPromptBuilderImpl @Inject constructor() : SystemPromptBuilder {

    // Block A is static — identical for every user and every call → high cache-hit rate.
    // It encodes the tutor role, correction directive, and the <say>/<meta> output contract.
    private val staticBlock: String = """
You are an encouraging English tutor helping Brazilian Portuguese speakers improve their spoken English.
Correct grammar, vocabulary, and fluency errors naturally within the conversation — inline, concise, non-punitive.
Always respond using this exact format:
<say>Your spoken English response with corrections woven in encouragingly.</say>
<meta>{"corrections":[{"cat":"GRAMMAR|VOCABULARY|FLUENCY","note":"brief note"}],"vocab":["word"],"pt":false}</meta>
The <say> block is spoken aloud; the <meta> block is machine-readable only. Use compact JSON keys.
If the user spoke Portuguese, set "pt":true in the meta block and gently encourage them to continue in English.
""".trim()

    override fun build(config: ConversationConfig): SystemPrompt {
        val personalizationBlock = buildPersonalizationBlock(config)
        return SystemPrompt(staticBlock, personalizationBlock)
    }

    private fun buildPersonalizationBlock(config: ConversationConfig): String {
        val sb = StringBuilder()
        sb.appendLine("You are ${config.persona.displayName}, the student's English tutor. Introduce yourself and refer to yourself by this name.")
        config.learningGoal?.let { sb.appendLine("Learning goal: ${it.displayLabel}") }
        if (config.themes.isNotEmpty()) {
            sb.appendLine("Conversation themes: ${config.themes.joinToString(", ") { it.displayLabel }}")
        }
        config.cefrHint?.let { sb.appendLine("Current CEFR level estimate: ${it.label}") }
        if (config.mode == SessionMode.DUAL) {
            val names = config.participants.joinToString(" and ") { it.name }
            sb.appendLine("This is a co-practice session with two speakers: $names. Address corrections to the active speaker by name.")
        }
        return sb.toString().trim()
    }
}
