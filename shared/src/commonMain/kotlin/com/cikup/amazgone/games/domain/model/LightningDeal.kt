package com.cikup.amazgone.games.domain.model

import com.cikup.amazgone.catalog.domain.model.Product

/**
 * One product per hourly window gets an extra discount (delivered as a coupon). Everyone sees the
 * same deal in the same window because the pick is derived from the window index, not randomness.
 */
data class LightningDeal(
    val product: Product,
    val extraPercent: Long,
    val windowStart: Long,
    val windowEnd: Long,
) {
    val couponCode: String get() = "FLASH-${windowStart / WINDOW_MILLIS}"

    fun remainingMillis(now: Long): Long = (windowEnd - now).coerceAtLeast(0)

    /** Simulated "% claimed" that rises through the window for urgency (never 100% before the end). */
    fun claimedFraction(now: Long): Float {
        val elapsed = ((now - windowStart).toFloat() / WINDOW_MILLIS).coerceIn(0f, 1f)
        return (BASE_CLAIMED + elapsed * (MAX_CLAIMED - BASE_CLAIMED))
    }

    companion object {
        const val WINDOW_MILLIS = 60 * 60 * 1_000L
        const val EXTRA_PERCENT = 20L
        private const val BASE_CLAIMED = 0.18f
        private const val MAX_CLAIMED = 0.94f

        fun forTime(candidates: List<Product>, now: Long): LightningDeal? {
            if (candidates.isEmpty()) return null
            val window = now / WINDOW_MILLIS
            val sorted = candidates.sortedBy { it.id }
            val product = sorted[(window % sorted.size).toInt()]
            val start = window * WINDOW_MILLIS
            return LightningDeal(product, EXTRA_PERCENT, start, start + WINDOW_MILLIS)
        }
    }
}
