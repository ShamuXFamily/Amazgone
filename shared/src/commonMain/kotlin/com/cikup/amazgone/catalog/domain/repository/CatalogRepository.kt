package com.cikup.amazgone.catalog.domain.repository

import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductWithReviews
import kotlinx.coroutines.flow.Flow

/** Local-first catalog: every observe* reads Room; network only writes into Room. */
interface CatalogRepository {
    fun observeProducts(categorySlug: String?): Flow<List<Product>>
    fun observeDeals(limit: Int): Flow<List<Product>>
    fun observeTopRated(limit: Int): Flow<List<Product>>
    fun observeCategories(): Flow<List<Category>>
    fun observeProduct(productId: String): Flow<ProductWithReviews?>
    fun observeSimilar(productId: String, categorySlug: String, limit: Int): Flow<List<Product>>
    /** FTS prefix search in relevance order; [ftsQuery] from ProductQueryEngine.toFtsQuery. */
    fun search(ftsQuery: String): Flow<List<Product>>
    suspend fun productsByIds(ids: List<String>): List<Product>
    /** Best-effort remote lookup that enriches the local catalog (e.g. more games). */
    suspend fun searchRemote(query: String)
}
