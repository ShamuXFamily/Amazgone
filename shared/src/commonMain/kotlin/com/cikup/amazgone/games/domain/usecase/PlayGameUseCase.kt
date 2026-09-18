package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.games.domain.model.Cooldowns
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.model.GamePlay
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.games.domain.model.RewardTables
import com.cikup.amazgone.games.domain.repository.GamesRepository
import com.cikup.amazgone.progress.domain.model.XpRules
import kotlin.random.Random

/** Result carries the reward index so the UI can animate the wheel onto the winning segment. */
data class PlayOutcome(val play: GamePlay, val rewardIndex: Int)

/** Enforces the cooldown, picks a weighted reward and records it (works offline). */
class PlayGameUseCase(
    private val games: GamesRepository,
    private val time: TimeProvider,
    private val ids: IdGenerator,
    private val random: Random = Random.Default,
) {
    suspend operator fun invoke(kind: GameKind): DomainResult<PlayOutcome> {
        val now = time.nowMillis()
        val remaining = Cooldowns.remaining(kind, games.lastPlayedAt(kind), now)
        if (remaining > 0) return DomainResult.Failure(DomainError.Cooldown(remaining))
        val table = RewardTables.forKind(kind)
        val index = RewardTables.pickIndex(table, random)
        val reward = table[index]
        val xp = baseXp(kind) + ((reward as? Reward.XpBoost)?.xp ?: 0)
        val play = GamePlay(ids.newId(), kind, index, xp, now)
        games.record(play)
        return DomainResult.Success(PlayOutcome(play, index))
    }

    private fun baseXp(kind: GameKind) = when (kind) {
        GameKind.SPIN -> XpRules.SPIN_XP
        GameKind.SCRATCH -> XpRules.SCRATCH_XP
    }
}
