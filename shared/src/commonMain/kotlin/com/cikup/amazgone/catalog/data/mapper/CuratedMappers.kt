package com.cikup.amazgone.catalog.data.mapper

import com.cikup.amazgone.catalog.data.remote.dto.CuratedProductDto
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.core.remote.FirestoreDocument
import com.cikup.amazgone.core.remote.list
import com.cikup.amazgone.core.remote.string
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

private val ID_PATTERN = Regex("[a-z0-9][a-z0-9-]{0,63}")
private const val MAX_TITLE = 200
private const val MAX_PRICE_USD = 100_000.0

/** Curated data is edited by hand, so every item is validated; bad items are skipped, never crash the sync. */
fun List<CuratedProductDto>.toCuratedBatch(now: Long): CatalogBatch =
    CatalogBatch(mapNotNull { it.toProductOrNull(now) }, emptyList())

fun CuratedProductDto.toProductOrNull(now: Long): Product? {
    val valid = published && ID_PATTERN.matches(id) && title.isNotBlank() && title.length <= MAX_TITLE &&
        category.isNotBlank() && priceUsd.isFinite() && priceUsd > 0 && priceUsd <= MAX_PRICE_USD &&
        (originalPriceUsd == null || (originalPriceUsd.isFinite() && originalPriceUsd > priceUsd))
    if (!valid) return null
    val safeImages = images.filter { it.startsWith("https://") }
    return Product(
        id = CatalogSourceId.AMAZGONE.productId(id),
        source = CatalogSourceId.AMAZGONE,
        title = title.trim(),
        description = description.trim(),
        brand = brand?.takeIf { it.isNotBlank() },
        categorySlug = category.trim().lowercase(),
        priceUsd = priceUsd,
        originalPriceUsd = originalPriceUsd,
        rating = null, // new launches have no reviews yet; never invent them
        ratingCount = null,
        stock = stock?.coerceAtLeast(0),
        availability = availability,
        thumbnailUrl = safeImages.firstOrNull().orEmpty(),
        imageUrls = safeImages,
        tags = tags,
        details = ProductDetails(
            warranty = warranty,
            shipping = shipping,
            returnPolicy = returnPolicy,
            releaseDateMillis = releaseDate?.let(::parseDateOrNull),
            highlights = highlights.filter { it.isNotBlank() },
            imageCredit = imageCredit?.takeIf { it.isNotBlank() },
        ),
        updatedAt = now,
    )
}

private fun parseDateOrNull(iso: String): Long? =
    runCatching { LocalDate.parse(iso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull()

/** Firestore `catalog/{id}` → DTO. The document id is the product id. */
fun FirestoreDocument.toCuratedDtoOrNull(): CuratedProductDto? {
    val f = fields
    val price = (f["priceUsd"] as? Number)?.toDouble() ?: return null
    return CuratedProductDto(
        id = id,
        title = f.string("title") ?: return null,
        brand = f.string("brand"),
        category = f.string("category") ?: return null,
        description = f.string("description").orEmpty(),
        priceUsd = price,
        originalPriceUsd = (f["originalPriceUsd"] as? Number)?.toDouble(),
        stock = (f["stock"] as? Number)?.toInt(),
        availability = f.string("availability"),
        images = f.list("images").filterIsInstance<String>(),
        imageCredit = f.string("imageCredit"),
        highlights = f.list("highlights").filterIsInstance<String>(),
        releaseDate = f.string("releaseDate"),
        warranty = f.string("warranty"),
        shipping = f.string("shipping"),
        returnPolicy = f.string("returnPolicy"),
        tags = f.list("tags").filterIsInstance<String>(),
        published = f["published"] as? Boolean ?: true,
    )
}
