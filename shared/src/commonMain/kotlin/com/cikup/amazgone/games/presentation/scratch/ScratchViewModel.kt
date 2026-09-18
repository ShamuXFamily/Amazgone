package com.cikup.amazgone.games.presentation.scratch

import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.usecase.ObserveCooldownUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveLatestPlayUseCase
import com.cikup.amazgone.games.domain.usecase.PlayGameUseCase
import com.cikup.amazgone.games.domain.usecase.PlayOutcome
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import kotlinx.coroutines.flow.combine

class ScratchViewModel(
    ticker: TickerUseCase,
    observeCooldown: ObserveCooldownUseCase,
    observeLatestPlay: ObserveLatestPlayUseCase,
    private val playGame: PlayGameUseCase,
) : MviViewModel<ScratchState, ScratchIntent, ScratchEffect>(ScratchState()) {

    init {
        // Returning during the cooldown shows the last card already revealed (its prize was granted at hand-out).
        // Both streams are combined so the restore never depends on which one emits first.
        combine(observeCooldown(GameKind.SCRATCH, ticker()), observeLatestPlay(GameKind.SCRATCH), ::Pair).observe { (remaining, latest) ->
            setState {
                val restore = card == null && latest != null && remaining > 0
                copy(
                    cooldown = remaining,
                    card = if (restore) PlayOutcome(latest!!, latest.rewardIndex) else card,
                    revealed = revealed || restore,
                )
            }
        }
    }

    override fun handleIntent(intent: ScratchIntent) {
        when (intent) {
            ScratchIntent.NewCard -> newCard()
            ScratchIntent.Revealed -> setState { copy(revealed = true) }
            ScratchIntent.Back -> sendEffect(ScratchEffect.NavigateBack)
        }
    }

    private fun newCard() {
        if (currentState.isLoading || currentState.cooldown > 0) return
        setState { copy(isLoading = true) }
        launchSafely {
            when (val result = playGame(GameKind.SCRATCH)) {
                is DomainResult.Success -> setState { copy(card = result.value, revealed = false, isLoading = false) }
                is DomainResult.Failure -> setState { copy(isLoading = false) }
            }
        }
    }

    override fun onError(throwable: Throwable) = setState { copy(isLoading = false) }
}
