package com.cikup.amazgone.wishlist.domain.usecase

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWishlistUseCase(
    private val wishlist: WishlistRepository,
    private val catalog: CatalogRepository,
) {
    operator fun invoke(): Flow<List<Product>> = wishlist.observeIds().mapLatest { ids ->
        val products = catalog.productsByIds(ids).associateBy { it.id }
        ids.mapNotNull { products[it] }
    }
}
