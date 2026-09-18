package com.cikup.amazgone.games.presentation.hub

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.games.domain.model.LightningDeal

data class GamesHubState(
    val xp: Long = 0,
    val coins: Long = 0,
    val spinCooldown: Long = 0,
    val scratchCooldown: Long = 0,
    val deal: LightningDeal? = null,
    val now: Long = 0,
) : UiState

enum class GameDestination { SPIN, SCRATCH, DEAL, LEADERBOARD, ACHIEVEMENTS }

sealed interface GamesHubIntent : UiIntent {
    data class Open(val destination: GameDestination) : GamesHubIntent
}

sealed interface GamesHubEffect : UiEffect {
    data class Navigate(val destination: GameDestination) : GamesHubEffect
}
