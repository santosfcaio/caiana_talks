package com.caiana.talks.ui.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LcarsIndicationTest {

    @Test
    fun `pressed is false initially`() = runTest {
        val source = MutableInteractionSource()
        val instance = LcarsIndicationInstance(source, TestScope(UnconfinedTestDispatcher()))
        assertFalse(instance.pressed)
    }

    @Test
    fun `pressed becomes true on PressInteraction Press`() = runTest(UnconfinedTestDispatcher()) {
        val source = MutableInteractionSource()
        val instance = LcarsIndicationInstance(source, backgroundScope)
        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        assertTrue(instance.pressed)
    }

    @Test
    fun `pressed becomes false on PressInteraction Release`() = runTest(UnconfinedTestDispatcher()) {
        val source = MutableInteractionSource()
        val instance = LcarsIndicationInstance(source, backgroundScope)
        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        source.emit(PressInteraction.Release(press))
        assertFalse(instance.pressed)
    }

    @Test
    fun `pressed becomes false on PressInteraction Cancel`() = runTest(UnconfinedTestDispatcher()) {
        val source = MutableInteractionSource()
        val instance = LcarsIndicationInstance(source, backgroundScope)
        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        source.emit(PressInteraction.Cancel(press))
        assertFalse(instance.pressed)
    }
}
