package com.cikup.amazgone.wishlist.domain.usecase

import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository

/** @return the new saved state. */
class ToggleWishlistUseCase(private val wishlist: WishlistRepository) {
    suspend operator fun invoke(productId: String): Boolean {
        val next = !wishlist.isSaved(productId)
        wishlist.setSaved(productId, next)
        return next
    }
}
