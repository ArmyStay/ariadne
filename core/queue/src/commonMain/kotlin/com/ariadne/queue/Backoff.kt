package com.ariadne.queue

/**
 * Capped exponential backoff for the extraction retry path.
 *
 * The schedule is fixed and published in the PRD (2s, 8s, 30s, 2m, 10m) because
 * a capture that fails in a dead zone must retry before the traveller reaches
 * the next wifi, not after.
 */
public object Backoff {

    public val scheduleMillis: List<Long> = listOf(
        2_000L,
        8_000L,
        30_000L,
        120_000L,
        600_000L,
    )

    /** Delay before the next attempt, given how many attempts have already failed. */
    public fun delayMillis(failedAttempts: Int): Long {
        if (failedAttempts <= 0) return scheduleMillis.first()
        val index = (failedAttempts - 1).coerceAtMost(scheduleMillis.lastIndex)
        return scheduleMillis[index]
    }

    /** Attempts after which the capture is left for the human rather than retried forever. */
    public const val MAX_ATTEMPTS: Int = 5

    public fun shouldRetry(failedAttempts: Int): Boolean = failedAttempts < MAX_ATTEMPTS
}
