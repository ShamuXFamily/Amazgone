package com.cikup.amazgone.games.presentation.hub

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.usecase.ObserveCooldownUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveLightningDealUseCase
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class GamesHubViewModel(
    ticker: TickerUseCase,
    observeCooldown: ObserveCooldownUseCase,
    observeDeal: ObserveLightningDealUseCase,
    observeProgress: ObserveProgressUseCase,
    observeWallet: ObserveWalletUseCase,
) : MviViewModel<GamesHubState, GamesHubIntent, GamesHubEffect>(GamesHubState()) {

    init {
        val ticks = ticker().shareIn(vmScope, SharingStarted.WhileSubscribed(), replay = 1)
        ticks.observe { setState { copy(now = it) } }
        observeCooldown(GameKind.SPIN, ticks).observe { setState { copy(spinCooldown = it) } }
        observeCooldown(GameKind.SCRATCH, ticks).observe { setState { copy(scratchCooldown = it) } }
        observeDeal(ticks).observe { setState { copy(deal = it) } }
        observeProgress().observe { setState { copy(xp = it.xp) } }
        observeWallet().observe { setState { copy(coins = it.coins) } }
    }

    override fun handleIntent(intent: GamesHubIntent) = when (intent) {
        is GamesHubIntent.Open -> sendEffect(GamesHubEffect.Navigate(intent.destination))
    }
}
