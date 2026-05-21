package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.SystemPromptBuilderImpl
import com.caiana.talks.domain.model.AiPersona
import com.caiana.talks.domain.model.CefrLevel
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.ConversationTheme
import com.caiana.talks.domain.model.LearningGoal
import com.caiana.talks.domain.model.ParticipantInfo
import com.caiana.talks.domain.model.SessionMode
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import com.caiana.talks.domain.model.VoicePreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemPromptBuilderTest {

    private lateinit var builder: SystemPromptBuilderImpl
    private lateinit var baseConfig: ConversationConfig

    @Before
    fun setUp() {
        builder = SystemPromptBuilderImpl()
        baseConfig = ConversationConfig(
            mode = SessionMode.SINGLE,
            participants = listOf(ParticipantInfo(1, "Caio")),
            learningGoal = LearningGoal.BUSINESS,
            themes = setOf(ConversationTheme.TOURISM),
            voice = VoicePreference(VoiceGender.MASCULINE, VoiceAccent.AMERICAN, SpeechRate.NORMAL),
            persona = AiPersona.MICHAEL,
            cefrHint = null
        )
    }

    @Test
    fun `static block is byte-identical across different personalization configs`() {
        val config1 = baseConfig.copy(cefrHint = CefrLevel.B1)
        val config2 = baseConfig.copy(
            participants = listOf(ParticipantInfo(2, "Ana")),
            learningGoal = LearningGoal.TRAVEL,
            cefrHint = CefrLevel.A2
        )
        val prompt1 = builder.build(config1)
        val prompt2 = builder.build(config2)
        assertTrue(prompt1.staticBlock == prompt2.staticBlock)
    }

    @Test
    fun `personalization block encodes learning goal`() {
        val prompt = builder.build(baseConfig)
        assertTrue(prompt.personalizationBlock.contains(LearningGoal.BUSINESS.displayLabel, ignoreCase = true))
    }

    @Test
    fun `personalization block encodes conversation theme`() {
        val prompt = builder.build(baseConfig)
        assertTrue(prompt.personalizationBlock.contains(ConversationTheme.TOURISM.displayLabel, ignoreCase = true))
    }

    @Test
    fun `personalization block omits CEFR when null`() {
        val prompt = builder.build(baseConfig.copy(cefrHint = null))
        assertFalse(prompt.personalizationBlock.contains("cefr", ignoreCase = true))
    }

    @Test
    fun `personalization block includes CEFR hint when present`() {
        val prompt = builder.build(baseConfig.copy(cefrHint = CefrLevel.B2))
        assertTrue(prompt.personalizationBlock.contains("B2", ignoreCase = true))
    }

    @Test
    fun `personalization block includes persona name Michael`() {
        val prompt = builder.build(baseConfig)
        assertTrue(prompt.personalizationBlock.contains("Michael"))
    }

    @Test
    fun `personalization block includes persona name Phoebe`() {
        val config = baseConfig.copy(
            voice = VoicePreference(VoiceGender.FEMININE, VoiceAccent.BRITISH, SpeechRate.NORMAL),
            persona = AiPersona.PHOEBE
        )
        val prompt = builder.build(config)
        assertTrue(prompt.personalizationBlock.contains("Phoebe"))
    }

    @Test
    fun `dual mode personalization block includes both participant names`() {
        val config = baseConfig.copy(
            mode = SessionMode.DUAL,
            participants = listOf(ParticipantInfo(1, "Caio"), ParticipantInfo(2, "Ana"))
        )
        val prompt = builder.build(config)
        assertTrue(prompt.personalizationBlock.contains("Caio"))
        assertTrue(prompt.personalizationBlock.contains("Ana"))
    }

    @Test
    fun `static block contains say and meta output contract markers`() {
        val prompt = builder.build(baseConfig)
        assertTrue(prompt.staticBlock.contains("<say>"))
        assertTrue(prompt.staticBlock.contains("<meta>"))
    }

    @Test
    fun `combined token estimate stays within budget`() {
        val prompt = builder.build(baseConfig.copy(cefrHint = CefrLevel.B1))
        val combined = prompt.staticBlock + prompt.personalizationBlock
        // Rough estimate: 4 chars per token
        val estimatedTokens = combined.length / 4
        assertTrue("Token estimate $estimatedTokens should be under 300", estimatedTokens < 300)
    }
}
