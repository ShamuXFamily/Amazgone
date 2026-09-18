package com.cikup.amazgone.orders.data

import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.cart.data.coupon.CouponRepositoryImpl
import com.cikup.amazgone.cart.data.repository.CartRepositoryImpl
import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderDraft
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.model.LedgerReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

const val OUTBOX_ORDER_PLACE = "order.place"

/** Outbox payload; also the shape stored remotely under users/{uid}/orders/{orderId}. */
@Serializable
data class OrderPayload(
    val orderId: String,
    val items: List<OrderItem>,
    val subtotalCoins: Long,
    val discountCoins: Long,
    val totalCoins: Long,
    val couponCode: String?,
    val address: ShippingAddress,
    val xpEarned: Long,
    val createdAt: Long,
)

class OrderRepositoryImpl(
    private val dao: OrderDao,
    private val wallet: WalletRepositoryImpl,
    private val cart: CartRepositoryImpl,
    private val coupons: CouponRepositoryImpl,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : OrderRepository, UserScopedStore {

    override fun observeOrders(): Flow<List<Order>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeOrder(orderId: String): Flow<Order?> = dao.observe(orderId).map { it?.toDomain() }

    override suspend fun placeOrder(draft: OrderDraft, usedCouponCode: String?): DomainResult<Order> = transactions.inTransaction {
        // Same transaction as the debit, so two concurrent checkouts can't both pass the check.
        val balance = wallet.balance(Currency.COINS)
        if (balance < draft.totalCoins) {
            return@inTransaction DomainResult.Failure(DomainError.InsufficientCoins(draft.totalCoins, balance))
        }
        val payload = OrderPayload(
            orderId = ids.newId(),
            items = draft.items,
            subtotalCoins = draft.subtotalCoins,
            discountCoins = draft.discountCoins,
            totalCoins = draft.totalCoins,
            couponCode = draft.couponCode,
            address = draft.address,
            xpEarned = draft.xpEarned,
            createdAt = time.nowMillis(),
        )
        val entity = payload.toEntity(OrderStatus.PENDING_SYNC)
        dao.insert(entity)
        wallet.recordPending(Currency.COINS, -draft.totalCoins, LedgerReason.PURCHASE, payload.orderId)
        wallet.recordPending(Currency.XP, draft.xpEarned, LedgerReason.ORDER_XP, payload.orderId)
        cart.clear()
        usedCouponCode?.let { coupons.markUsed(it) }
        outbox.enqueue(payload.orderId, OUTBOX_ORDER_PLACE, AppJson.encodeToString(OrderPayload.serializer(), payload))
        DomainResult.Success(entity.toDomain())
    }

    suspend fun markConfirmed(orderId: String) = transactions.inTransaction {
        dao.updateStatus(orderId, OrderStatus.CONFIRMED.name, null)
        wallet.confirm(orderId)
    }

    /** Server refused: refund (reject ledger rows), give the coupon back and put the items back in the cart. */
    suspend fun markRejected(orderId: String, reason: String) = transactions.inTransaction {
        val order = dao.find(orderId) ?: return@inTransaction
        dao.updateStatus(orderId, OrderStatus.REJECTED.name, reason)
        wallet.reject(orderId)
        order.couponCode?.let { coupons.markUnused(it) }
        cart.restore(order.items().map { CartEntry(it.productId, it.quantity, 0) })
    }

    /** Orders placed on another device arrive through the puller. */
    suspend fun upsertRemote(payload: OrderPayload) {
        if (dao.find(payload.orderId) == null) dao.insert(payload.toEntity(OrderStatus.CONFIRMED))
    }

    override suspend fun clearUserData() = dao.deleteAll()
}

private val itemsSerializer = ListSerializer(OrderItem.serializer())

private fun OrderPayload.toEntity(status: OrderStatus) = OrderEntity(
    id = orderId,
    status = status.name,
    subtotalCoins = subtotalCoins,
    discountCoins = discountCoins,
    totalCoins = totalCoins,
    couponCode = couponCode,
    addressJson = AppJson.encodeToString(ShippingAddress.serializer(), address),
    itemsJson = AppJson.encodeToString(itemsSerializer, items),
    xpEarned = xpEarned,
    createdAt = createdAt,
    rejectionReason = null,
)

private fun OrderEntity.items(): List<OrderItem> = AppJson.decodeFromString(itemsSerializer, itemsJson)

private fun OrderEntity.toDomain() = Order(
    id = id,
    items = items(),
    subtotalCoins = subtotalCoins,
    discountCoins = discountCoins,
    totalCoins = totalCoins,
    couponCode = couponCode,
    address = AppJson.decodeFromString(ShippingAddress.serializer(), addressJson),
    status = OrderStatus.valueOf(status),
    xpEarned = xpEarned,
    createdAt = createdAt,
    rejectionReason = rejectionReason,
)
