package com.cikup.amazgone.orders.presentation.list

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.Order

data class OrdersState(val orders: List<Order> = emptyList(), val isLoading: Boolean = true) : UiState

sealed interface OrdersIntent : UiIntent {
    data class OpenOrder(val orderId: String) : OrdersIntent
    data object Back : OrdersIntent
}

sealed interface OrdersEffect : UiEffect {
    data class NavigateToOrder(val orderId: String) : OrdersEffect
    data object NavigateBack : OrdersEffect
}
