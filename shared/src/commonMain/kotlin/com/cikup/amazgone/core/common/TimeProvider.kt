package com.cikup.amazgone.core.common

import kotlin.time.Clock

/** Injectable clock so time-based rules (TTL, cooldowns, backoff) are testable. */
fun interface TimeProvider {
    fun nowMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
