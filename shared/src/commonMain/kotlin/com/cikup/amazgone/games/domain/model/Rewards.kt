package com.cikup.amazgone.games.domain.model

import com.cikup.amazgone.cart.domain.model.CouponKind
import kotlin.random.Random

enum class GameKind(val cooldownMillis: Long) {
    SPIN(24 * HOUR),
    SCRATCH(4 * HOUR),
}

private const val HOUR = 60 * 60 * 1_000L

/** What a play can award. Every reward also grants the game's XP. */
sealed interface Reward {
    val weight: Int

    data class Coins(val amount: Long, override val weight: Int) : Reward
    data class CouponReward(val kind: CouponKind, val value: Long, val minSubtotalCoins: Long, override val weight: Int) : Reward
    data class XpBoost(val xp: Long, override val weight: Int) : Reward
}

data class GamePlay(val id: String, val kind: GameKind, val rewardIndex: Int, val xp: Long, val playedAt: Long) {
    val reward: Reward get() = RewardTables.forKind(kind)[rewardIndex]
}

/**
 * Weighted reward tables. Weights are relative (sum need not be 100). Expected value per spin
 * is ~118 coins, kept well below cheap items so games feel generous without breaking the economy.
 */
object RewardTables {
    val SPIN: List<Reward> = listOf(
        Reward.Coins(50, weight = 30),
        Reward.Coins(100, weight = 25),
        Reward.CouponReward(CouponKind.PERCENT, 10, minSubtotalCoins = 500, weight = 15),
        Reward.Coins(250, weight = 12),
        Reward.XpBoost(100, weight = 10),
        Reward.CouponReward(CouponKind.FLAT_COINS, 300, minSubtotalCoins = 1_500, weight = 5),
        Reward.Coins(1_000, weight = 2),
        Reward.CouponReward(CouponKind.PERCENT, 25, minSubtotalCoins = 2_000, weight = 1),
    )

    val SCRATCH: List<Reward> = listOf(
        Reward.Coins(25, weight = 40),
        Reward.Coins(75, weight = 25),
        Reward.CouponReward(CouponKind.PERCENT, 5, minSubtotalCoins = 300, weight = 20),
        Reward.XpBoost(50, weight = 10),
        Reward.Coins(400, weight = 5),
    )

    /** Weighted pick; returns the index so the wheel can land on the matching segment. */
    fun pickIndex(table: List<Reward>, random: Random): Int {
        val total = table.sumOf { it.weight }
        require(total > 0) { "Reward table needs positive weights" }
        var roll = random.nextInt(total)
        table.forEachIndexed { index, reward ->
            if (roll < reward.weight) return index
            roll -= reward.weight
        }
        return table.lastIndex
    }

    fun forKind(kind: GameKind): List<Reward> = when (kind) {
        GameKind.SPIN -> SPIN
        GameKind.SCRATCH -> SCRATCH
    }
}

object Cooldowns {
    /** Millis until [kind] can be played again (0 = available now). */
    fun remaining(kind: GameKind, lastPlayedAt: Long?, now: Long): Long =
        if (lastPlayedAt == null) 0 else (lastPlayedAt + kind.cooldownMillis - now).coerceAtLeast(0)
}
