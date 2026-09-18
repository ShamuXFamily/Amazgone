package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductQueryEngine
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Offline search: FTS candidates (or the whole catalog for an empty query) + filters + sort. */
class SearchProductsUseCase(private val repository: CatalogRepository) {
    operator fun invoke(query: String, filters: SearchFilters, sort: SortOrder): Flow<List<Product>> {
        val fts = ProductQueryEngine.toFtsQuery(query)
        val candidates = if (fts == null) repository.observeProducts(filters.categorySlug) else repository.search(fts)
        return candidates.map { ProductQueryEngine.apply(it, filters, sort) }
    }
}
