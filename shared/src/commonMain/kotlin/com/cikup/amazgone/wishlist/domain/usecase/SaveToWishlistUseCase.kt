package com.cikup.amazgone.wishlist.domain.usecase

import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository

/** Idempotent save (unlike toggle): used by "Save for later" in the cart. */
class SaveToWishlistUseCase(private val wishlist: WishlistRepository) {
    suspend operator fun invoke(productId: String) = wishlist.setSaved(productId, saved = true)
}
