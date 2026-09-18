package com.cikup.amazgone.orders.presentation.detail

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.Order

data class OrderDetailState(val order: Order? = null, val isLoading: Boolean = true) : UiState

sealed interface OrderDetailIntent : UiIntent {
    data object Back : OrderDetailIntent
    data class OpenProduct(val productId: String) : OrderDetailIntent
}

sealed interface OrderDetailEffect : UiEffect {
    data object NavigateBack : OrderDetailEffect
    data class NavigateToProduct(val productId: String) : OrderDetailEffect
}
