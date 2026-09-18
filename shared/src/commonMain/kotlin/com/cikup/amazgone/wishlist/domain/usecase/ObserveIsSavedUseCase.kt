package com.cikup.amazgone.wishlist.domain.usecase

import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveIsSavedUseCase(private val wishlist: WishlistRepository) {
    operator fun invoke(productId: String): Flow<Boolean> =
        wishlist.observeIds().map { productId in it }.distinctUntilChanged()
}
