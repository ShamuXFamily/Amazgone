package com.cikup.amazgone.core.sync.domain

/** Exponential backoff for outbox retries: 2s, 4s, 8s … capped at 15 minutes. */
object Backoff {
    const val BASE_MILLIS = 2_000L
    const val MAX_MILLIS = 15 * 60 * 1_000L
    /** After this many failed attempts an entry is treated as rejected so it cannot block the queue forever. */
    const val MAX_ATTEMPTS = 12

    fun delayMillis(attempts: Int): Long {
        if (attempts <= 0) return 0L
        val shift = (attempts - 1).coerceAtMost(MAX_SHIFT)
        return (BASE_MILLIS shl shift).coerceAtMost(MAX_MILLIS)
    }

    private const val MAX_SHIFT = 20
}
