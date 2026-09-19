package com.cikup.amazgone.catalog.data.local

import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.catalog.domain.model.Review

fun Product.toEntity() = ProductEntity(
    id = id,
    source = source.key,
    title = title,
    description = description,
    brand = brand,
    categorySlug = categorySlug,
    priceUsd = priceUsd,
    originalPriceUsd = originalPriceUsd,
    rating = rating,
    ratingCount = ratingCount,
    stock = stock,
    availability = availability,
    thumbnailUrl = thumbnailUrl,
    imageUrls = imageUrls,
    tags = tags,
    warranty = details.warranty,
    shipping = details.shipping,
    returnPolicy = details.returnPolicy,
    minimumOrderQuantity = details.minimumOrderQuantity,
    sku = details.sku,
    metacriticScore = details.metacriticScore,
    ratingLabel = details.ratingLabel,
    releaseDateMillis = details.releaseDateMillis,
    highlights = details.highlights,
    imageCredit = details.imageCredit,
    sellerId = details.sellerId,
    storeId = store.id,
    storeName = store.name,
    storeKind = store.kind.name,
    updatedAt = updatedAt,
)

fun ProductEntity.toDomain() = Product(
    id = id,
    source = CatalogSourceId.entries.firstOrNull { it.key == source } ?: CatalogSourceId.DUMMY_JSON,
    title = title,
    description = description,
    brand = brand,
    categorySlug = categorySlug,
    priceUsd = priceUsd,
    originalPriceUsd = originalPriceUsd,
    rating = rating,
    ratingCount = ratingCount,
    stock = stock,
    availability = availability,
    thumbnailUrl = thumbnailUrl,
    imageUrls = imageUrls,
    tags = tags,
    details = ProductDetails(
        warranty = warranty,
        shipping = shipping,
        returnPolicy = returnPolicy,
        minimumOrderQuantity = minimumOrderQuantity,
        sku = sku,
        metacriticScore = metacriticScore,
        ratingLabel = ratingLabel,
        releaseDateMillis = releaseDateMillis,
        highlights = highlights,
        imageCredit = imageCredit,
        sellerId = sellerId,
    ),
    updatedAt = updatedAt,
)

fun Review.toEntity(position: Int) = ReviewEntity(productId, position, reviewerName, rating, comment, dateMillis)

fun ReviewEntity.toDomain() = Review(productId, reviewerName, rating, comment, dateMillis)

fun CategoryRow.toDomain() = Category(slug, productCount)
