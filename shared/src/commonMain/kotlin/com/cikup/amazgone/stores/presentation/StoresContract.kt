package com.cikup.amazgone.stores.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.model.StoreSummary

data class StoresState(
    val isLoading: Boolean = true,
    val sections: Map<StoreKind, List<StoreSummary>> = emptyMap(),
) : UiState

sealed interface StoresIntent : UiIntent {
    data class OpenStore(val storeId: String) : StoresIntent
    data object Back : StoresIntent
}

sealed interface StoresEffect : UiEffect {
    data class NavigateToStore(val storeId: String) : StoresEffect
    data object NavigateBack : StoresEffect
}
