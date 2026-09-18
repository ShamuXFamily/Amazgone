package com.cikup.amazgone.progress.domain.model

import kotlin.math.sqrt

/**
 * Level curve: level L needs 100·(L−1)² XP in total (L2 = 100, L3 = 400, L5 = 1,600, L10 = 8,100).
 * Quadratic keeps early levels quick and later ones meaningful.
 */
object LevelCurve {
    private const val XP_SCALE = 100.0
    const val MAX_LEVEL = 99

    fun levelFor(xp: Long): Int {
        if (xp <= 0) return 1
        return (sqrt(xp / XP_SCALE).toInt() + 1).coerceAtMost(MAX_LEVEL)
    }

    fun xpForLevel(level: Int): Long {
        val n = (level.coerceIn(1, MAX_LEVEL) - 1).toLong()
        return (XP_SCALE * n * n).toLong()
    }

    /** 0..1 progress towards the next level. */
    fun progress(xp: Long): Float {
        val level = levelFor(xp)
        if (level >= MAX_LEVEL) return 1f
        val start = xpForLevel(level)
        val end = xpForLevel(level + 1)
        return ((xp - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }
}

/** XP awarded for each activity. Server-side writes use the same numbers. */
object XpRules {
    const val SPIN_XP = 25L
    const val SCRATCH_XP = 15L
    const val DAILY_LOGIN_XP = 20L
    private const val COINS_PER_XP = 10L
    private const val MIN_ORDER_XP = 10L
    const val MAX_XP_PER_WRITE = 5_000L

    fun forOrder(totalCoins: Long): Long = (totalCoins / COINS_PER_XP).coerceIn(MIN_ORDER_XP, MAX_XP_PER_WRITE)
}
