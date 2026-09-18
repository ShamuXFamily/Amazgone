package com.cikup.amazgone.orders.domain.repository

import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderDraft
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    fun observeOrder(orderId: String): Flow<Order?>
    /**
     * Atomically (one local transaction): re-checks the balance, stores the order as PENDING_SYNC,
     * debits coins and credits XP as pending ledger rows, empties the cart and queues the server push.
     */
    suspend fun placeOrder(draft: OrderDraft, usedCouponCode: String?): DomainResult<Order>
}
