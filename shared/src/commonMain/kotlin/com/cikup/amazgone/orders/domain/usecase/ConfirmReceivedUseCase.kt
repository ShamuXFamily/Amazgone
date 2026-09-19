package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.orders.domain.model.canConfirmReceived
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.first

/** "Order received": only while a confirmed parcel is on its way; unlocks reviews for its items. */
class ConfirmReceivedUseCase(private val orders: OrderRepository, private val time: TimeProvider) {
    suspend operator fun invoke(orderId: String): Boolean {
        val now = time.nowMillis()
        val order = orders.observeOrder(orderId).first() ?: return false
        if (!order.canConfirmReceived(now)) return false
        orders.markReceived(orderId, now)
        return true
    }
}
