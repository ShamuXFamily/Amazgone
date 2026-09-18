package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartEntry
import com.cikup.amazgone.cart.domain.repository.CartRepository

/** Undo for swipe-to-delete, and cart recovery when an offline order is rejected. */
class RestoreCartUseCase(private val cart: CartRepository) {
    suspend operator fun invoke(entries: List<CartEntry>) = cart.restore(entries)
}
