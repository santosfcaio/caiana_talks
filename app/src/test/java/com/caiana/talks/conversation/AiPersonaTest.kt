package com.caiana.talks.conversation

import com.caiana.talks.domain.model.AiPersona
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import org.junit.Assert.assertEquals
import org.junit.Test

class AiPersonaTest {

    @Test
    fun `masculine american resolves to Michael`() {
        val persona = AiPersona.of(VoiceGender.MASCULINE, VoiceAccent.AMERICAN)
        assertEquals(AiPersona.MICHAEL, persona)
        assertEquals("Michael", persona.displayName)
    }

    @Test
    fun `masculine british resolves to David`() {
        val persona = AiPersona.of(VoiceGender.MASCULINE, VoiceAccent.BRITISH)
        assertEquals(AiPersona.DAVID, persona)
        assertEquals("David", persona.displayName)
    }

    @Test
    fun `feminine american resolves to Mary`() {
        val persona = AiPersona.of(VoiceGender.FEMININE, VoiceAccent.AMERICAN)
        assertEquals(AiPersona.MARY, persona)
        assertEquals("Mary", persona.displayName)
    }

    @Test
    fun `feminine british resolves to Phoebe`() {
        val persona = AiPersona.of(VoiceGender.FEMININE, VoiceAccent.BRITISH)
        assertEquals(AiPersona.PHOEBE, persona)
        assertEquals("Phoebe", persona.displayName)
    }

    @Test
    fun `of resolver is total - all four combinations resolve`() {
        VoiceGender.entries.forEach { gender ->
            VoiceAccent.entries.forEach { accent ->
                val persona = AiPersona.of(gender, accent)
                assertEquals(gender, persona.gender)
                assertEquals(accent, persona.accent)
            }
        }
    }
}
