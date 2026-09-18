package com.cikup.amazgone.games.presentation.deal

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.games.domain.model.LightningDeal

data class DealState(val deal: LightningDeal? = null, val now: Long = 0, val isClaiming: Boolean = false) : UiState {
    val remaining: Long get() = deal?.remainingMillis(now) ?: 0
    val dealPriceCoins: Long get() = deal?.let { it.product.priceCoins * (PERCENT - it.extraPercent) / PERCENT } ?: 0

    private companion object {
        const val PERCENT = 100
    }
}

sealed interface DealIntent : UiIntent {
    data object Claim : DealIntent
    data object OpenProduct : DealIntent
    data object Back : DealIntent
}

sealed interface DealEffect : UiEffect {
    data class Claimed(val couponCode: String) : DealEffect
    data class NavigateToProduct(val productId: String) : DealEffect
    data object NavigateBack : DealEffect
}
