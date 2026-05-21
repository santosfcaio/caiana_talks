package com.caiana.talks.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LcarsColorsTest {

    @Test
    fun `black is fully black`() {
        assertEquals(Color(0xFF000000), LcarsColors.Black)
    }

    @Test
    fun `orange has correct hex`() {
        assertEquals(Color(0xFFFF7700), LcarsColors.Orange)
    }

    @Test
    fun `blue has correct hex`() {
        assertEquals(Color(0xFF6688CC), LcarsColors.Blue)
    }

    @Test
    fun `purple has correct hex`() {
        assertEquals(Color(0xFFCC99CC), LcarsColors.Purple)
    }

    @Test
    fun `beige has correct hex`() {
        assertEquals(Color(0xFFFFCC88), LcarsColors.Beige)
    }

    @Test
    fun `maroon has correct hex`() {
        assertEquals(Color(0xFFCC2200), LcarsColors.Maroon)
    }

    @Test
    fun `text has correct hex`() {
        assertEquals(Color(0xFFFFEECC), LcarsColors.Text)
    }

    @Test
    fun `textDim has correct hex`() {
        assertEquals(Color(0xFF997755), LcarsColors.TextDim)
    }

    @Test
    fun `surface has correct hex`() {
        assertEquals(Color(0xFF111111), LcarsColors.Surface)
    }

    @Test
    fun `no two tokens share the same value`() {
        val tokens = listOf(
            LcarsColors.Black, LcarsColors.Orange, LcarsColors.Blue,
            LcarsColors.Purple, LcarsColors.Beige, LcarsColors.Maroon,
            LcarsColors.Text, LcarsColors.TextDim, LcarsColors.Surface,
        )
        assertEquals("All tokens must be unique", tokens.size, tokens.toSet().size)
    }

    @Test
    fun `black is pure black with no alpha`() {
        assertTrue(LcarsColors.Black.red == 0f)
        assertTrue(LcarsColors.Black.green == 0f)
        assertTrue(LcarsColors.Black.blue == 0f)
        assertTrue(LcarsColors.Black.alpha == 1f)
    }
}
