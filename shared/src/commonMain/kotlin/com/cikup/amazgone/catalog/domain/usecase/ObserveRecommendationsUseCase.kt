package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * "Customers also bought": best-rated products from the same category, topped up with
 * overall top-rated items when the category is small. Never includes the product itself.
 */
class ObserveRecommendationsUseCase(private val repository: CatalogRepository) {
    operator fun invoke(product: Product, limit: Int = DEFAULT_LIMIT): Flow<List<Product>> = combine(
        repository.observeSimilar(product.id, product.categorySlug, limit),
        repository.observeTopRated(limit + 1),
    ) { similar, topRated ->
        (similar + topRated)
            .asSequence()
            .filter { it.id != product.id }
            .distinctBy { it.id }
            .take(limit)
            .toList()
    }

    private companion object {
        const val DEFAULT_LIMIT = 10
    }
}
