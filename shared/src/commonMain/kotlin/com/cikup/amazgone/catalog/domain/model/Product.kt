package com.cikup.amazgone.catalog.domain.model

/** Which upstream API a product came from; part of every product id ("{source}:{id}"). */
enum class CatalogSourceId(val key: String) {
    DUMMY_JSON("dummyjson"),
    CHEAP_SHARK("cheapshark"),
    /** Hand-curated new arrivals (Firestore `catalog` collection + bundled starter set). */
    AMAZGONE("amazgone"),
    ;

    fun productId(rawId: String): String = "$key:$rawId"

    companion object {
        fun fromProductId(productId: String): CatalogSourceId? =
            entries.firstOrNull { productId.startsWith("${it.key}:") }
    }
}

data class Product(
    val id: String,
    val source: CatalogSourceId,
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
    val details: ProductDetails,
    val updatedAt: Long,
) {
    val priceCoins: Long get() = CoinPricing.toCoins(priceUsd)
    val originalPriceCoins: Long? get() = originalPriceUsd?.let(CoinPricing::toCoins)
    /** From coin prices (what the player actually pays), so the 1-coin minimum never shows as -100%. */
    val discountPercent: Int get() = CoinPricing.discountPercent(priceCoins.toDouble(), originalPriceCoins?.toDouble())
    val isInStock: Boolean get() = stock == null || stock > 0

    /** Announced but not released yet: sold as a pre-order until its release day. */
    fun isPreorderAt(nowMillis: Long): Boolean = details.releaseDateMillis?.let { it > nowMillis } ?: false
}

/** Optional spec sheet; sources fill what they have and the UI hides the rest. */
data class ProductDetails(
    val warranty: String? = null,
    val shipping: String? = null,
    val returnPolicy: String? = null,
    val minimumOrderQuantity: Int? = null,
    val sku: String? = null,
    val metacriticScore: Int? = null,
    val ratingLabel: String? = null,
    val releaseDateMillis: Long? = null,
    /** Short "About this item" bullet points. */
    val highlights: List<String> = emptyList(),
    /** Attribution required by the image licence (e.g. CC BY-SA), shown under the gallery. */
    val imageCredit: String? = null,
)

data class Review(
    val productId: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val dateMillis: Long,
)

data class Category(val slug: String, val productCount: Int)

/** Everything the detail screen needs in one emission. */
data class ProductWithReviews(val product: Product, val reviews: List<Review>)
