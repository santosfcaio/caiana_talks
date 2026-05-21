package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.SessionDurationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDurationPolicyTest {

    @Test
    fun `59 seconds is discarded`() {
        assertFalse(SessionDurationPolicy.shouldKeep(59_000L))
    }

    @Test
    fun `60 seconds exactly is kept`() {
        assertTrue(SessionDurationPolicy.shouldKeep(60_000L))
    }

    @Test
    fun `61 seconds is kept`() {
        assertTrue(SessionDurationPolicy.shouldKeep(61_000L))
    }

    @Test
    fun `0 ms is discarded`() {
        assertFalse(SessionDurationPolicy.shouldKeep(0L))
    }

    @Test
    fun `negative duration is discarded`() {
        assertFalse(SessionDurationPolicy.shouldKeep(-1L))
    }

    @Test
    fun `one hour is kept`() {
        assertTrue(SessionDurationPolicy.shouldKeep(3_600_000L))
    }

    @Test
    fun `59999 ms is discarded`() {
        assertFalse(SessionDurationPolicy.shouldKeep(59_999L))
    }
}
