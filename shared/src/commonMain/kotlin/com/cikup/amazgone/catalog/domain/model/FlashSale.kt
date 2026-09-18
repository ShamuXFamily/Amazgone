package com.cikup.amazgone.catalog.domain.model

/** A discounted product in the current flash window with its simulated sell-through. */
data class FlashDeal(val product: Product, val soldFraction: Float) {
    val soldPercent: Int get() = (soldFraction * PERCENT).toInt()

    private companion object {
        const val PERCENT = 100
    }
}

/** Hourly flash-sale windows: the previous one is done, the current one counts down, the next is announced. */
data class FlashSale(
    val windowStart: Long,
    val windowEnd: Long,
    val deals: List<FlashDeal>,
) {
    val previousStart: Long get() = windowStart - FlashSaleClock.WINDOW_MILLIS
    val nextStart: Long get() = windowEnd

    fun remainingMillis(now: Long): Long = (windowEnd - now).coerceAtLeast(0)
}

/** Pure timing rules so every device shows the same windows and the same "% sold" for a product. */
object FlashSaleClock {
    const val WINDOW_MILLIS = 60 * 60 * 1_000L
    private const val BASE_MIN = 0.15f
    private const val BASE_SPREAD = 0.45f
    private const val WINDOW_GROWTH = 0.35f
    private const val MAX_SOLD = 0.97f
    private const val HASH_BUCKETS = 1_000

    fun windowStart(now: Long): Long = now - now.mod(WINDOW_MILLIS)

    /** Starts somewhere between 15–60% (stable per product/window) and climbs during the hour. */
    fun soldFraction(productId: String, now: Long): Float {
        val start = windowStart(now)
        val seed = (productId.hashCode() xor (start / WINDOW_MILLIS).toInt()).mod(HASH_BUCKETS) / HASH_BUCKETS.toFloat()
        val elapsed = (now - start).toFloat() / WINDOW_MILLIS
        return (BASE_MIN + seed * BASE_SPREAD + elapsed * WINDOW_GROWTH).coerceAtMost(MAX_SOLD)
    }

    fun sale(deals: List<Product>, now: Long): FlashSale {
        val start = windowStart(now)
        return FlashSale(start, start + WINDOW_MILLIS, deals.map { FlashDeal(it, soldFraction(it.id, now)) })
    }
}
