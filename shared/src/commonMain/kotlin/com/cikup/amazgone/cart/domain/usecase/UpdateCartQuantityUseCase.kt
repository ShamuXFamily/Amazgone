package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.repository.CartRepository

/** Sets an absolute quantity; 0 removes the item. */
class UpdateCartQuantityUseCase(private val cart: CartRepository) {
    suspend operator fun invoke(productId: String, quantity: Int) =
        cart.setQuantity(productId, CartCalculator.clampQuantity(quantity))
}
