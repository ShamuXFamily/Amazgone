package com.cikup.amazgone.cart.data.repository

import com.cikup.amazgone.cart.data.local.CartDao
import com.cikup.amazgone.cart.data.local.CartItemEntity
import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.repository.CartRepository
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.sync.domain.OutboxStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class CartSetPayload(val productId: String, val quantity: Int, val updatedAt: Long)

const val OUTBOX_CART_SET = "cart.set"

class CartRepositoryImpl(
    private val dao: CartDao,
    private val outbox: OutboxStore,
    private val transactions: TransactionRunner,
    private val time: TimeProvider,
    private val ids: IdGenerator,
) : CartRepository {

    override fun observeEntries(): Flow<List<CartEntry>> =
        dao.observeActive().map { rows -> rows.map { CartEntry(it.productId, it.quantity, it.updatedAt) } }

    override suspend fun quantityOf(productId: String): Int = dao.find(productId)?.quantity ?: 0

    override suspend fun setQuantity(productId: String, quantity: Int) = transactions.inTransaction {
        write(productId, quantity, time.nowMillis())
    }

    override suspend fun clear() = transactions.inTransaction {
        val now = time.nowMillis()
        dao.active().forEach { write(it.productId, 0, now) }
    }

    override suspend fun restore(entries: List<CartEntry>) = transactions.inTransaction {
        val now = time.nowMillis()
        entries.forEach { write(it.productId, it.quantity, now) }
    }

    /** Applies a remote value only if it is newer (last-write-wins); used by the cart puller. */
    suspend fun mergeRemote(productId: String, quantity: Int, updatedAt: Long) {
        val local = dao.find(productId)
        if (local != null && local.updatedAt >= updatedAt) return
        dao.upsert(CartItemEntity(productId, quantity, local?.addedAt ?: updatedAt, updatedAt))
    }

    suspend fun snapshot(): List<CartEntry> = dao.all().map { CartEntry(it.productId, it.quantity, it.updatedAt) }

    private suspend fun write(productId: String, quantity: Int, now: Long) {
        val existing = dao.find(productId)
        val addedAt = if (existing == null || existing.quantity == 0) now else existing.addedAt
        dao.upsert(CartItemEntity(productId, quantity, addedAt, now))
        outbox.enqueue(ids.newId(), OUTBOX_CART_SET, AppJson.encodeToString(CartSetPayload.serializer(), CartSetPayload(productId, quantity, now)))
    }
}
