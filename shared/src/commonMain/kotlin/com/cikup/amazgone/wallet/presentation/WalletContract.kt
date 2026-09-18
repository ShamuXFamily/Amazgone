package com.cikup.amazgone.wallet.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.wallet.domain.model.WalletSummary

data class WalletState(val summary: WalletSummary = WalletSummary(0, 0, emptyList()), val isLoading: Boolean = true) : UiState

sealed interface WalletIntent : UiIntent {
    data object Back : WalletIntent
}

sealed interface WalletEffect : UiEffect {
    data object NavigateBack : WalletEffect
}
