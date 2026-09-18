package com.cikup.amazgone.progress.domain.model

/** Aggregates the achievement rules look at. */
data class PlayerStats(
    val orders: Int = 0,
    val coinsSpent: Long = 0,
    val spins: Int = 0,
    val scratches: Int = 0,
    val level: Int = 1,
    val wishlistSize: Int = 0,
)

enum class AchievementId(val target: Long, val measure: (PlayerStats) -> Long) {
    FIRST_ORDER(1, { it.orders.toLong() }),
    SHOPAHOLIC(10, { it.orders.toLong() }),
    BIG_SPENDER(10_000, { it.coinsSpent }),
    LUCKY_SPIN(1, { it.spins.toLong() }),
    WHEEL_REGULAR(7, { it.spins.toLong() }),
    SCRATCHER(5, { it.scratches.toLong() }),
    RISING_STAR(5, { it.level.toLong() }),
    LEGEND(10, { it.level.toLong() }),
    COLLECTOR(5, { it.wishlistSize.toLong() }),
}

data class Achievement(val id: AchievementId, val progress: Long, val unlockedAt: Long?) {
    val isUnlocked: Boolean get() = unlockedAt != null
    val fraction: Float get() = (progress.toFloat() / id.target).coerceIn(0f, 1f)
}

/** Pure rules: which achievements the stats satisfy. */
object AchievementEngine {
    fun earned(stats: PlayerStats): Set<AchievementId> =
        AchievementId.entries.filter { it.measure(stats) >= it.target }.toSet()

    /** Merges persisted unlocks with live progress; unlocks are permanent once stored. */
    fun board(stats: PlayerStats, unlockedAt: Map<AchievementId, Long>): List<Achievement> =
        AchievementId.entries.map { id -> Achievement(id, id.measure(stats).coerceAtMost(id.target), unlockedAt[id]) }
}

data class LeaderboardEntry(val uid: String, val username: String, val xp: Long, val level: Int, val rank: Int)

data class Leaderboard(val entries: List<LeaderboardEntry>, val syncedAt: Long?, val myUid: String?) {
    val myEntry: LeaderboardEntry? get() = entries.firstOrNull { it.uid == myUid }
}
