package com.cikup.amazgone.catalog.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Upsert
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM reviews WHERE productId IN (:productIds)")
    suspend fun deleteReviewsFor(productIds: List<String>)

    @Upsert
    suspend fun upsertReviews(reviews: List<ReviewEntity>)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query(
        """SELECT * FROM products WHERE (:category IS NULL OR categorySlug = :category)
           ORDER BY COALESCE(rating, 0) DESC, title ASC""",
    )
    fun observeProducts(category: String?): Flow<List<ProductEntity>>

    @Query(
        """SELECT * FROM products WHERE originalPriceUsd IS NOT NULL AND originalPriceUsd > priceUsd
           ORDER BY (1.0 - priceUsd / originalPriceUsd) DESC LIMIT :limit""",
    )
    fun observeDeals(limit: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE source = :source ORDER BY COALESCE(releaseDateMillis, 0) DESC, title ASC LIMIT :limit")
    fun observeBySourceNewestFirst(source: String, limit: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE rating IS NOT NULL ORDER BY rating DESC, COALESCE(ratingCount, 0) DESC LIMIT :limit")
    fun observeTopRated(limit: Int): Flow<List<ProductEntity>>

    @Query("SELECT categorySlug AS slug, COUNT(*) AS productCount FROM products GROUP BY categorySlug ORDER BY productCount DESC, slug ASC")
    fun observeCategories(): Flow<List<CategoryRow>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY dateMillis DESC, position ASC")
    fun observeReviews(productId: String): Flow<List<ReviewEntity>>

    @Query(
        """SELECT * FROM products WHERE categorySlug = :category AND id != :excludeId
           ORDER BY COALESCE(rating, 0) DESC LIMIT :limit""",
    )
    fun observeSimilar(excludeId: String, category: String, limit: Int): Flow<List<ProductEntity>>

    @Query(
        """SELECT products.* FROM products JOIN products_fts ON products.rowid = products_fts.rowid
           WHERE products_fts MATCH :query ORDER BY COALESCE(products.rating, 0) DESC LIMIT :limit""",
    )
    fun search(query: String, limit: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<ProductEntity>
}
