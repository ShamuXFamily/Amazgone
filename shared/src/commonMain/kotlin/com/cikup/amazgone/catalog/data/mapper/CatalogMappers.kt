package com.cikup.amazgone.catalog.data.mapper

import com.cikup.amazgone.catalog.data.remote.dto.CheapSharkDealDto
import com.cikup.amazgone.catalog.data.remote.dto.CheapSharkGameDto
import com.cikup.amazgone.catalog.data.remote.dto.DummyProductDto
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.catalog.domain.model.Review
import kotlin.time.Instant

/** Products and reviews from one upstream fetch, ready to be written to Room. */
data class CatalogBatch(val products: List<Product>, val reviews: List<Review>) {
    companion object {
        val EMPTY = CatalogBatch(emptyList(), emptyList())
    }
}

const val VIDEO_GAMES_CATEGORY = "video-games"
private const val MIN_MEANINGFUL_DISCOUNT = 1.0
private const val PERCENT = 100.0
private const val STEAM_PERCENT_PER_STAR = 20.0
private const val SECONDS_TO_MILLIS = 1_000L

@kotlin.jvm.JvmName("dummyToBatch")
fun List<DummyProductDto>.toBatch(now: Long): CatalogBatch = CatalogBatch(
    products = map { it.toProduct(now) },
    reviews = flatMap { dto -> dto.reviews.map { it.toReview(CatalogSourceId.DUMMY_JSON.productId(dto.id.toString())) } },
)

fun DummyProductDto.toProduct(now: Long): Product {
    val discount = discountPercentage ?: 0.0
    val original = if (discount >= MIN_MEANINGFUL_DISCOUNT && discount < PERCENT) price / (1 - discount / PERCENT) else null
    return Product(
        id = CatalogSourceId.DUMMY_JSON.productId(id.toString()),
        source = CatalogSourceId.DUMMY_JSON,
        title = title,
        description = description,
        brand = brand,
        categorySlug = category,
        priceUsd = price,
        originalPriceUsd = original,
        rating = rating,
        ratingCount = reviews.size.takeIf { it > 0 },
        stock = stock,
        availability = availabilityStatus,
        thumbnailUrl = thumbnail,
        imageUrls = images.ifEmpty { listOf(thumbnail) },
        tags = tags,
        details = ProductDetails(
            warranty = warrantyInformation,
            shipping = shippingInformation,
            returnPolicy = returnPolicy,
            minimumOrderQuantity = minimumOrderQuantity,
            sku = sku,
        ),
        updatedAt = now,
    )
}

private fun com.cikup.amazgone.catalog.data.remote.dto.DummyReviewDto.toReview(productId: String) = Review(
    productId = productId,
    reviewerName = reviewerName,
    rating = rating,
    comment = comment,
    dateMillis = date?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L,
)

/** CheapShark lists one deal per store; keep the cheapest per game. */
@kotlin.jvm.JvmName("dealsToBatch")
fun List<CheapSharkDealDto>.toBatch(now: Long): CatalogBatch = CatalogBatch(
    products = groupBy { it.gameID }
        .map { (_, deals) -> deals.minBy { it.salePrice.toDoubleOrNull() ?: Double.MAX_VALUE } }
        .map { it.toProduct(now) },
    reviews = emptyList(),
)

fun CheapSharkDealDto.toProduct(now: Long): Product {
    val sale = salePrice.toDoubleOrNull() ?: 0.0
    val normal = normalPrice?.toDoubleOrNull()?.takeIf { it > sale }
    val steamPercent = steamRatingPercent?.toDoubleOrNull()?.takeIf { it > 0 }
    return Product(
        id = CatalogSourceId.CHEAP_SHARK.productId(gameID),
        source = CatalogSourceId.CHEAP_SHARK,
        title = title,
        description = "",
        brand = null,
        categorySlug = VIDEO_GAMES_CATEGORY,
        priceUsd = sale,
        originalPriceUsd = normal,
        rating = steamPercent?.let { it / STEAM_PERCENT_PER_STAR },
        ratingCount = steamRatingCount?.toIntOrNull()?.takeIf { it > 0 },
        stock = null,
        availability = null,
        thumbnailUrl = thumb,
        imageUrls = listOfNotNull(steamHeaderUrl(steamAppID), thumb.takeIf { it.isNotBlank() }),
        tags = listOf(VIDEO_GAMES_CATEGORY),
        details = ProductDetails(
            metacriticScore = metacriticScore?.toIntOrNull()?.takeIf { it > 0 },
            ratingLabel = steamRatingText,
            releaseDateMillis = releaseDate?.takeIf { it > 0 }?.times(SECONDS_TO_MILLIS),
        ),
        updatedAt = now,
    )
}

fun CheapSharkGameDto.toProduct(now: Long): Product {
    val price = cheapest.toDoubleOrNull() ?: 0.0
    return Product(
        id = CatalogSourceId.CHEAP_SHARK.productId(gameID),
        source = CatalogSourceId.CHEAP_SHARK,
        title = external,
        description = "",
        brand = null,
        categorySlug = VIDEO_GAMES_CATEGORY,
        priceUsd = price,
        originalPriceUsd = null,
        rating = null,
        ratingCount = null,
        stock = null,
        availability = null,
        thumbnailUrl = thumb,
        imageUrls = listOfNotNull(steamHeaderUrl(steamAppID), thumb.takeIf { it.isNotBlank() }),
        tags = listOf(VIDEO_GAMES_CATEGORY),
        details = ProductDetails(),
        updatedAt = now,
    )
}

/** Steam's 460x215 header art is far sharper than CheapShark's 231x87 capsule. */
private fun steamHeaderUrl(steamAppId: String?): String? =
    steamAppId?.takeIf { it.isNotBlank() && it != "0" }
        ?.let { "https://cdn.akamai.steamstatic.com/steam/apps/$it/header.jpg" }
