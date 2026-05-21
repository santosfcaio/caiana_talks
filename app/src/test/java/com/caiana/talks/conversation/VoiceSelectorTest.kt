package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.VoiceDescriptor
import com.caiana.talks.data.conversation.VoiceSelector
import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class VoiceSelectorTest {

    @Test
    fun `localeFor AMERICAN returns Locale US`() {
        assertEquals(Locale.US, VoiceSelector.localeFor(VoiceAccent.AMERICAN))
    }

    @Test
    fun `localeFor BRITISH returns Locale UK`() {
        assertEquals(Locale.UK, VoiceSelector.localeFor(VoiceAccent.BRITISH))
    }

    @Test
    fun `rateFor SLOW returns 0_8f`() {
        assertEquals(0.8f, VoiceSelector.rateFor(SpeechRate.SLOW), 0.001f)
    }

    @Test
    fun `rateFor NORMAL returns 1_0f`() {
        assertEquals(1.0f, VoiceSelector.rateFor(SpeechRate.NORMAL), 0.001f)
    }

    @Test
    fun `rateFor FAST returns 1_4f`() {
        assertEquals(1.4f, VoiceSelector.rateFor(SpeechRate.FAST), 0.001f)
    }

    @Test
    fun `pickVoice returns feminine voice when available`() {
        val candidates = listOf(
            VoiceDescriptor("en-us-male", Locale.US, isFeminine = false),
            VoiceDescriptor("en-us-female", Locale.US, isFeminine = true)
        )
        val result = VoiceSelector.pickVoice(candidates, VoiceGender.FEMININE, Locale.US)
        assertNotNull(result)
        assertEquals("en-us-female", result!!.name)
    }

    @Test
    fun `pickVoice returns masculine voice when available`() {
        val candidates = listOf(
            VoiceDescriptor("en-us-male", Locale.US, isFeminine = false),
            VoiceDescriptor("en-us-female", Locale.US, isFeminine = true)
        )
        val result = VoiceSelector.pickVoice(candidates, VoiceGender.MASCULINE, Locale.US)
        assertNotNull(result)
        assertEquals("en-us-male", result!!.name)
    }

    @Test
    fun `pickVoice returns null when no candidates`() {
        val result = VoiceSelector.pickVoice(emptyList(), VoiceGender.FEMININE, Locale.US)
        assertNull(result)
    }

    @Test
    fun `pickVoice returns null when no voice matches locale`() {
        val candidates = listOf(
            VoiceDescriptor("en-gb-female", Locale.UK, isFeminine = true)
        )
        val result = VoiceSelector.pickVoice(candidates, VoiceGender.FEMININE, Locale.US)
        assertNull(result)
    }

    @Test
    fun `pickVoice falls back to any locale-matching voice when no gendered match`() {
        val candidates = listOf(
            VoiceDescriptor("en-us-generic", Locale.US, isFeminine = false)
        )
        // Requesting FEMININE but only masculine available — returns the locale match as fallback
        val result = VoiceSelector.pickVoice(candidates, VoiceGender.FEMININE, Locale.US)
        assertNotNull(result)
    }
}
