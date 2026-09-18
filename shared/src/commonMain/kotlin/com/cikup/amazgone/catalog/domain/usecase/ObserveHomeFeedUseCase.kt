package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.HomeFeed
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHomeFeedUseCase(private val repository: CatalogRepository) {
    operator fun invoke(categorySlug: String?): Flow<HomeFeed> = combine(
        repository.observeDeals(DEALS_LIMIT),
        repository.observeTopRated(TOP_RATED_LIMIT),
        repository.observeCategories(),
        repository.observeProducts(categorySlug),
        repository.observeNewArrivals(NEW_ARRIVALS_LIMIT),
    ) { deals, topRated, categories, products, newArrivals -> HomeFeed(deals, topRated, categories, products, newArrivals) }

    private companion object {
        const val DEALS_LIMIT = 8
        const val TOP_RATED_LIMIT = 12
        const val NEW_ARRIVALS_LIMIT = 10
    }
}
