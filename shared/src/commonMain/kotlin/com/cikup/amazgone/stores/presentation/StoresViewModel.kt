package com.cikup.amazgone.stores.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.stores.domain.usecase.ObserveStoresUseCase

class StoresViewModel(observeStores: ObserveStoresUseCase) : MviViewModel<StoresState, StoresIntent, StoresEffect>(StoresState()) {

    init {
        observeStores().observe { setState { copy(isLoading = false, sections = it) } }
    }

    override fun handleIntent(intent: StoresIntent) {
        when (intent) {
            is StoresIntent.OpenStore -> sendEffect(StoresEffect.NavigateToStore(intent.storeId))
            StoresIntent.Back -> sendEffect(StoresEffect.NavigateBack)
        }
    }
}
