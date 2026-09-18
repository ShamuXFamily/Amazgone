package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveQuantityInCartUseCase(private val cart: CartRepository) {
    operator fun invoke(productId: String): Flow<Int> = cart.observeEntries()
        .map { entries -> entries.firstOrNull { it.productId == productId }?.quantity ?: 0 }
        .distinctUntilChanged()
}
