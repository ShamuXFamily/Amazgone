package com.cikup.amazgone.stores.domain.usecase

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductQueryEngine
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.stores.domain.model.StoreSummary
import com.cikup.amazgone.stores.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One store's shelf: header stats, its categories, and the products for the chosen category and sort. */
data class StorePage(val summary: StoreSummary?, val categories: List<String>, val products: List<Product>)

class ObserveStorePageUseCase(private val stores: StoreRepository) {
    operator fun invoke(storeId: String, category: Flow<String?>, sort: Flow<SortOrder>): Flow<StorePage> = combine(
        stores.observeStores(),
        stores.observeProducts(storeId),
        category,
        sort,
    ) { all, products, selected, order ->
        StorePage(
            summary = all.firstOrNull { it.store.id == storeId },
            categories = products.groupingBy { it.categorySlug }.eachCount().entries.sortedByDescending { it.value }.map { it.key },
            products = ProductQueryEngine.apply(products, SearchFilters(categorySlug = selected), order),
        )
    }
}
