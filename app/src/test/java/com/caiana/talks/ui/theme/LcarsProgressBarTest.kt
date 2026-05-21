package com.caiana.talks.ui.theme

import com.caiana.talks.ui.theme.components.activeSegmentCount
import org.junit.Assert.assertEquals
import org.junit.Test

class LcarsProgressBarTest {

    @Test
    fun `segment 0 is active at time 0`() {
        assertEquals(0, activeSegmentCount(0L, 12))
    }

    @Test
    fun `segment n is active at time n times 250ms`() {
        assertEquals(3, activeSegmentCount(3 * 250L, 12))
        assertEquals(7, activeSegmentCount(7 * 250L, 12))
        assertEquals(11, activeSegmentCount(11 * 250L, 12))
    }

    @Test
    fun `all segments active at segmentCount times 250ms`() {
        val segmentCount = 12
        assertEquals(segmentCount, activeSegmentCount(segmentCount * 250L, segmentCount))
    }

    @Test
    fun `loop restarts to 0 after segmentCount plus one steps`() {
        val segmentCount = 12
        val fullCycle = (segmentCount + 1) * 250L
        assertEquals(0, activeSegmentCount(fullCycle, segmentCount))
    }

    @Test
    fun `second cycle produces same values as first`() {
        val segmentCount = 8
        val fullCycle = (segmentCount + 1) * 250L
        for (step in 0..segmentCount) {
            val t = step * 250L
            assertEquals(activeSegmentCount(t, segmentCount), activeSegmentCount(t + fullCycle, segmentCount))
        }
    }

    @Test
    fun `works with a single segment`() {
        assertEquals(0, activeSegmentCount(0L, 1))
        assertEquals(1, activeSegmentCount(250L, 1))
        assertEquals(0, activeSegmentCount(500L, 1))
    }
}
