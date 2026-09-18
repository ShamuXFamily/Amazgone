package com.cikup.amazgone.games.presentation.deal

import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.ClaimLightningDealUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveLightningDealUseCase
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class DealViewModel(
    ticker: TickerUseCase,
    observeDeal: ObserveLightningDealUseCase,
    private val claimDeal: ClaimLightningDealUseCase,
) : MviViewModel<DealState, DealIntent, DealEffect>(DealState()) {

    init {
        val ticks = ticker().shareIn(vmScope, SharingStarted.WhileSubscribed(), replay = 1)
        ticks.observe { setState { copy(now = it) } }
        observeDeal(ticks).observe { setState { copy(deal = it) } }
    }

    override fun handleIntent(intent: DealIntent) {
        when (intent) {
            DealIntent.Claim -> claim()
            DealIntent.OpenProduct -> currentState.deal?.let { sendEffect(DealEffect.NavigateToProduct(it.product.id)) }
            DealIntent.Back -> sendEffect(DealEffect.NavigateBack)
        }
    }

    private fun claim() {
        val deal = currentState.deal ?: return
        if (currentState.isClaiming) return
        setState { copy(isClaiming = true) }
        launchSafely {
            if (claimDeal(deal) is DomainResult.Success) sendEffect(DealEffect.Claimed(deal.couponCode))
            setState { copy(isClaiming = false) }
        }
    }

    override fun onError(throwable: Throwable) = setState { copy(isClaiming = false) }
}
