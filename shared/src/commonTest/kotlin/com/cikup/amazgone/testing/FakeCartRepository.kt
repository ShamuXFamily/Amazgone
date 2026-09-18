package com.cikup.amazgone.testing

import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCartRepository(private val clock: FakeClock = FakeClock()) : CartRepository {
    val entries = MutableStateFlow<List<CartEntry>>(emptyList())

    override fun observeEntries() = entries.map { all -> all.filter { it.quantity > 0 } }

    override suspend fun quantityOf(productId: String) = entries.value.firstOrNull { it.productId == productId }?.quantity ?: 0

    override suspend fun setQuantity(productId: String, quantity: Int) {
        val others = entries.value.filterNot { it.productId == productId }
        entries.value = if (quantity > 0) others + CartEntry(productId, quantity, clock.now) else others
    }

    override suspend fun clear() { entries.value = emptyList() }

    override suspend fun restore(entries: List<CartEntry>) = entries.forEach { setQuantity(it.productId, it.quantity) }
}
