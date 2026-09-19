package com.cikup.amazgone.games.presentation.spin

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.usecase.ObserveCooldownUseCase
import com.cikup.amazgone.games.domain.usecase.PlayGameUseCase
import com.cikup.amazgone.games.domain.usecase.PlayOutcome
import com.cikup.amazgone.games.domain.usecase.TickerUseCase

class SpinViewModel(
    ticker: TickerUseCase,
    observeCooldown: ObserveCooldownUseCase,
    private val playGame: PlayGameUseCase,
    private val analytics: Analytics,
) : MviViewModel<SpinState, SpinIntent, SpinEffect>(SpinState()) {

    private var pending: PlayOutcome? = null

    init {
        observeCooldown(GameKind.SPIN, ticker()).observe { remaining ->
            // keep the wheel usable until the landing animation has finished
            if (!currentState.isSpinning) setState { copy(cooldown = remaining) }
        }
    }

    override fun handleIntent(intent: SpinIntent) {
        when (intent) {
            SpinIntent.Spin -> spin()
            SpinIntent.Landed -> pending?.let { outcome ->
                (outcome.play.reward as? Reward.Coins)?.let { analytics.earnCoins(it.amount, "spin") }
                setState { copy(isSpinning = false, wonReward = outcome.play.reward, wonXp = outcome.play.xp) }
                pending = null
            }
            SpinIntent.Back -> sendEffect(SpinEffect.NavigateBack)
        }
    }

    private fun spin() {
        if (!currentState.canSpin) return
        setState { copy(isSpinning = true, wonReward = null) }
        launchSafely {
            when (val result = playGame(GameKind.SPIN)) {
                is DomainResult.Success -> {
                    pending = result.value
                    setState { copy(targetIndex = result.value.rewardIndex, spinId = spinId + 1) }
                }
                is DomainResult.Failure -> setState { copy(isSpinning = false) }
            }
        }
    }

    override fun onError(throwable: Throwable) = setState { copy(isSpinning = false) }
}
