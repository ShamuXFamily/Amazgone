package com.cikup.amazgone.cart.domain.repository

import com.cikup.amazgone.cart.domain.model.CartEntry
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    /** Live, non-removed entries, oldest first. */
    fun observeEntries(): Flow<List<CartEntry>>
    suspend fun quantityOf(productId: String): Int
    /** Sets the absolute quantity (0 removes). Writes locally + enqueues a sync mutation atomically. */
    suspend fun setQuantity(productId: String, quantity: Int)
    suspend fun clear()
    /** Restores several entries at once (undo, rejected order). */
    suspend fun restore(entries: List<CartEntry>)
}
