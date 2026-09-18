package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.model.GamePlay
import com.cikup.amazgone.games.domain.repository.GamesRepository
import kotlinx.coroutines.flow.Flow

class ObserveLatestPlayUseCase(private val games: GamesRepository) {
    operator fun invoke(kind: GameKind): Flow<GamePlay?> = games.observeLatestPlay(kind)
}
