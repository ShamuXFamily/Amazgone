package com.cikup.amazgone.catalog.domain.model

import kotlin.math.ceil
import kotlin.math.roundToInt

/** Converts upstream USD prices into the in-app coin currency. Coins are the only money. */
object CoinPricing {
    const val COINS_PER_USD = 10

    /** Minimum price: nothing is free (a 0-coin order could not be told apart from "no purchase" by the rules). */
    const val MIN_PRICE_COINS = 1L

    fun toCoins(usd: Double): Long = ceil(usd.coerceAtLeast(0.0) * COINS_PER_USD).toLong().coerceAtLeast(MIN_PRICE_COINS)

    fun discountPercent(price: Double, original: Double?): Int {
        if (original == null || original <= 0.0 || price >= original) return 0
        return ((1 - price / original) * PERCENT).roundToInt()
    }

    private const val PERCENT = 100
}
