package com.cikup.amazgone.orders.presentation.detail

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.orders.domain.usecase.ObserveOrderUseCase

class OrderDetailViewModel(orderId: String, observeOrder: ObserveOrderUseCase) :
    MviViewModel<OrderDetailState, OrderDetailIntent, OrderDetailEffect>(OrderDetailState()) {

    init {
        observeOrder(orderId).observe { setState { copy(order = it, isLoading = false) } }
    }

    override fun handleIntent(intent: OrderDetailIntent) = when (intent) {
        OrderDetailIntent.Back -> sendEffect(OrderDetailEffect.NavigateBack)
        is OrderDetailIntent.OpenProduct -> sendEffect(OrderDetailEffect.NavigateToProduct(intent.productId))
    }
}
