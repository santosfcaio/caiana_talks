package com.caiana.talks.data.conversation

object SessionDurationPolicy {
    private const val MIN_DURATION_MS = 60_000L

    fun shouldKeep(durationMs: Long): Boolean = durationMs >= MIN_DURATION_MS
}
