package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.repository.CartRepository
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason

/** Adds [quantity] more of a product; fails when the per-item limit is already reached. */
class AddToCartUseCase(private val cart: CartRepository) {
    suspend operator fun invoke(productId: String, quantity: Int = 1): DomainResult<Int> {
        val current = cart.quantityOf(productId)
        if (current >= CartCalculator.MAX_QUANTITY_PER_ITEM) {
            return DomainResult.Failure(DomainError.Validation("quantity", ValidationReason.TOO_LONG))
        }
        val next = CartCalculator.clampQuantity(current + quantity)
        cart.setQuantity(productId, next)
        return DomainResult.Success(next)
    }
}
