package com.caiana.talks.conversation

import com.caiana.talks.ui.conversation.SpectrumWaveform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumWaveformTest {

    @Test
    fun `speaking false returns all bars at floor height`() {
        val bars = SpectrumWaveform.barHeights(0L, 24, speaking = false)
        assertEquals(24, bars.size)
        val floor = bars.first()
        assertTrue(bars.all { it == floor })
    }

    @Test
    fun `speaking true all heights within 0 to 1`() {
        val bars = SpectrumWaveform.barHeights(1000L, 24, speaking = true)
        bars.forEach { h ->
            assertTrue("Height $h must be >= 0", h >= 0f)
            assertTrue("Height $h must be <= 1", h <= 1f)
        }
    }

    @Test
    fun `speaking true bars are not all identical`() {
        val bars = SpectrumWaveform.barHeights(1000L, 24, speaking = true)
        val distinct = bars.distinct()
        assertTrue("Bars should have more than one distinct value", distinct.size > 1)
    }

    @Test
    fun `same timeMs and barCount produces same result - deterministic`() {
        val bars1 = SpectrumWaveform.barHeights(5000L, 12, speaking = true)
        val bars2 = SpectrumWaveform.barHeights(5000L, 12, speaking = true)
        assertEquals(bars1, bars2)
    }

    @Test
    fun `barCount parameter is respected`() {
        val bars = SpectrumWaveform.barHeights(0L, 16, speaking = false)
        assertEquals(16, bars.size)
    }

    @Test
    fun `barCount 1 is respected`() {
        val bars = SpectrumWaveform.barHeights(0L, 1, speaking = true)
        assertEquals(1, bars.size)
    }

    @Test
    fun `floor height is greater than zero`() {
        val bars = SpectrumWaveform.barHeights(0L, 24, speaking = false)
        assertTrue(bars.first() > 0f)
    }
}
