package com.cikup.amazgone.wallet.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase

class WalletViewModel(observeWallet: ObserveWalletUseCase) : MviViewModel<WalletState, WalletIntent, WalletEffect>(WalletState()) {
    init {
        observeWallet().observe { setState { copy(summary = it, isLoading = false) } }
    }

    override fun handleIntent(intent: WalletIntent) = when (intent) {
        WalletIntent.Back -> sendEffect(WalletEffect.NavigateBack)
    }
}
