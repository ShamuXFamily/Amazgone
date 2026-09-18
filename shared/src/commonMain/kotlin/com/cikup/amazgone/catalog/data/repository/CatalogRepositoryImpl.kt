package com.cikup.amazgone.catalog.data.repository

import com.cikup.amazgone.catalog.data.local.CatalogDao
import com.cikup.amazgone.catalog.data.local.toDomain
import com.cikup.amazgone.catalog.data.local.toEntity
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.remote.CatalogSource
import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductWithReviews
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.database.TransactionRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl(
    private val dao: CatalogDao,
    private val sources: List<CatalogSource>,
    private val transactions: TransactionRunner,
    private val logger: AppLogger,
) : CatalogRepository {

    override fun observeProducts(categorySlug: String?): Flow<List<Product>> =
        dao.observeProducts(categorySlug).map { rows -> rows.map { it.toDomain() } }

    override fun observeDeals(limit: Int) = dao.observeDeals(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeTopRated(limit: Int) = dao.observeTopRated(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { rows -> rows.map { it.toDomain() } }

    override fun observeProduct(productId: String): Flow<ProductWithReviews?> =
        combine(dao.observeProduct(productId), dao.observeReviews(productId)) { product, reviews ->
            product?.let { ProductWithReviews(it.toDomain(), reviews.map { r -> r.toDomain() }) }
        }

    override fun observeSimilar(productId: String, categorySlug: String, limit: Int) =
        dao.observeSimilar(productId, categorySlug, limit).map { rows -> rows.map { it.toDomain() } }

    override fun search(ftsQuery: String) = dao.search(ftsQuery, SEARCH_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override suspend fun productsByIds(ids: List<String>): List<Product> =
        if (ids.isEmpty()) emptyList() else dao.byIds(ids).map { it.toDomain() }

    /** Only adds products we do not know yet, so thin search results never overwrite rich deal data. */
    override suspend fun searchRemote(query: String) {
        sources.forEach { source ->
            try {
                val batch = source.search(query)
                val known = dao.byIds(batch.products.map { it.id }).map { it.id }.toSet()
                save(CatalogBatch(batch.products.filterNot { it.id in known }, emptyList()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                logger.error(TAG, "Remote search on ${source.id.key} failed", t)
            }
        }
    }

    /** Writes a batch atomically; reviews for the batch's products are replaced, not merged. */
    suspend fun save(batch: CatalogBatch) {
        if (batch.products.isEmpty()) return
        transactions.inTransaction {
            dao.upsertProducts(batch.products.map { it.toEntity() })
            if (batch.reviews.isNotEmpty()) {
                dao.deleteReviewsFor(batch.reviews.map { it.productId }.distinct())
                dao.upsertReviews(
                    batch.reviews.groupBy { it.productId }
                        .flatMap { (_, reviews) -> reviews.mapIndexed { index, review -> review.toEntity(index) } },
                )
            }
        }
    }

    suspend fun isEmpty(): Boolean = dao.count() == 0

    private companion object {
        const val TAG = "CatalogRepository"
        const val SEARCH_LIMIT = 200
    }
}
