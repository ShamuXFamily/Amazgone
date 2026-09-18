package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow

class ObserveOrdersUseCase(private val orders: OrderRepository) {
    operator fun invoke(): Flow<List<Order>> = orders.observeOrders()
}
