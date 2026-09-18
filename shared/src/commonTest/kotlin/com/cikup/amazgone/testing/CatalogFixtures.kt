package com.cikup.amazgone.testing

import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.catalog.domain.model.ProductWithReviews
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun product(
    id: String,
    price: Double = 10.0,
    category: String = "phones",
    rating: Double? = 4.0,
    original: Double? = null,
    stock: Int? = 5,
    title: String = "Product $id",
) = Product(
    id = "dummyjson:$id",
    source = CatalogSourceId.DUMMY_JSON,
    title = title,
    description = "",
    brand = null,
    categorySlug = category,
    priceUsd = price,
    originalPriceUsd = original,
    rating = rating,
    ratingCount = 1,
    stock = stock,
    availability = null,
    thumbnailUrl = "",
    imageUrls = emptyList(),
    tags = emptyList(),
    details = ProductDetails(),
    updatedAt = 0,
)

/** In-memory catalog; `search` does a naive case-insensitive title match. */
class FakeCatalogRepository(initial: List<Product> = emptyList()) : CatalogRepository {
    val products = MutableStateFlow(initial)
    val remoteQueries = mutableListOf<String>()

    override fun observeProducts(categorySlug: String?) =
        products.map { all -> all.filter { categorySlug == null || it.categorySlug == categorySlug } }

    override fun observeDeals(limit: Int) =
        products.map { all -> all.filter { it.discountPercent > 0 }.sortedByDescending { it.discountPercent }.take(limit) }

    override fun observeTopRated(limit: Int) =
        products.map { all -> all.sortedByDescending { it.rating ?: 0.0 }.take(limit) }

    override fun observeCategories(): Flow<List<Category>> =
        products.map { all -> all.groupingBy { it.categorySlug }.eachCount().map { (k, v) -> Category(k, v) } }

    override fun observeProduct(productId: String) =
        products.map { all -> all.firstOrNull { it.id == productId }?.let { ProductWithReviews(it, emptyList()) } }

    override fun observeSimilar(productId: String, categorySlug: String, limit: Int) =
        products.map { all -> all.filter { it.categorySlug == categorySlug && it.id != productId }.take(limit) }

    override fun search(ftsQuery: String) = products.map { all ->
        val needle = ftsQuery.removeSuffix("*").substringBefore("*")
        all.filter { it.title.lowercase().contains(needle) }
    }

    override suspend fun productsByIds(ids: List<String>) = products.value.filter { it.id in ids }

    override suspend fun searchRemote(query: String) {
        remoteQueries += query
    }
}
