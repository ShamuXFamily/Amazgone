package com.cikup.amazgone.orders.presentation.list

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.orders.domain.usecase.ObserveOrdersUseCase

class OrdersViewModel(observeOrders: ObserveOrdersUseCase, ticker: TickerUseCase) :
    MviViewModel<OrdersState, OrdersIntent, OrdersEffect>(OrdersState()) {
    init {
        observeOrders().observe { setState { copy(orders = it, isLoading = false) } }
        ticker(TICK_MS).observe { setState { copy(now = it) } }
    }

    private companion object {
        const val TICK_MS = 60_000L
    }

    override fun handleIntent(intent: OrdersIntent) = when (intent) {
        is OrdersIntent.OpenOrder -> sendEffect(OrdersEffect.NavigateToOrder(intent.orderId))
        OrdersIntent.Back -> sendEffect(OrdersEffect.NavigateBack)
    }
}
