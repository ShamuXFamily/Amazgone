package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow

class ObserveOrderUseCase(private val orders: OrderRepository) {
    operator fun invoke(orderId: String): Flow<Order?> = orders.observeOrder(orderId)
}
