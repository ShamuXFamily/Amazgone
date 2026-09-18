package com.cikup.amazgone.orders.presentation.list

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.orders.domain.usecase.ObserveOrdersUseCase

class OrdersViewModel(observeOrders: ObserveOrdersUseCase) : MviViewModel<OrdersState, OrdersIntent, OrdersEffect>(OrdersState()) {
    init {
        observeOrders().observe { setState { copy(orders = it, isLoading = false) } }
    }

    override fun handleIntent(intent: OrdersIntent) = when (intent) {
        is OrdersIntent.OpenOrder -> sendEffect(OrdersEffect.NavigateToOrder(intent.orderId))
        OrdersIntent.Back -> sendEffect(OrdersEffect.NavigateBack)
    }
}
