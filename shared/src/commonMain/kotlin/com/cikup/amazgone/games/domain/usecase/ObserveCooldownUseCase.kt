package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.games.domain.model.Cooldowns
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.repository.GamesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Remaining cooldown for [kind], re-evaluated on every [ticks] emission (e.g. each second). */
class ObserveCooldownUseCase(private val games: GamesRepository, private val time: TimeProvider) {
    operator fun invoke(kind: GameKind, ticks: Flow<Long>): Flow<Long> =
        combine(games.observeLastPlayedAt(kind), ticks) { last, _ -> Cooldowns.remaining(kind, last, time.nowMillis()) }
}
