package com.cikup.amazgone.wishlist.domain.usecase

import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveWishlistIdsUseCase(private val wishlist: WishlistRepository) {
    operator fun invoke(): Flow<Set<String>> = wishlist.observeIds().map { it.toSet() }
}
