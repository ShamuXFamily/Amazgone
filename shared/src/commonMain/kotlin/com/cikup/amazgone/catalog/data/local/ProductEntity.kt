package com.cikup.amazgone.catalog.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index("categorySlug"), Index("rating"), Index("storeId")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val source: String,
    val title: String,
    val description: String,
    val brand: String?,
    val categorySlug: String,
    val priceUsd: Double,
    val originalPriceUsd: Double?,
    val rating: Double?,
    val ratingCount: Int?,
    val stock: Int?,
    val availability: String?,
    val thumbnailUrl: String,
    val imageUrls: List<String>,
    val tags: List<String>,
    val warranty: String?,
    val shipping: String?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int?,
    val sku: String?,
    val metacriticScore: Int?,
    val ratingLabel: String?,
    val releaseDateMillis: Long?,
    val highlights: List<String>,
    val imageCredit: String?,
    val sellerId: String?,
    /** Derived from the product on write (StoreDirectory) so stores can be listed and filtered in SQL. */
    val storeId: String,
    val storeName: String,
    val storeKind: String,
    val updatedAt: Long,
)

/** External-content FTS index over [ProductEntity]; Room keeps it in sync with triggers. */
@Fts4(contentEntity = ProductEntity::class)
@Entity(tableName = "products_fts")
data class ProductFtsEntity(
    val title: String,
    val brand: String?,
    val categorySlug: String,
    val description: String,
    val tags: List<String>,
)

@Entity(tableName = "reviews", primaryKeys = ["productId", "position"], indices = [Index("productId")])
data class ReviewEntity(
    val productId: String,
    val position: Int,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val dateMillis: Long,
)

/** Row shape for the category aggregate query. */
data class CategoryRow(val slug: String, val productCount: Int)

/** Row shape for the store aggregate query. */
data class StoreRow(
    val storeId: String,
    val storeName: String,
    val storeKind: String,
    val productCount: Int,
    val averageRating: Double?,
    val topCategory: String?,
)
