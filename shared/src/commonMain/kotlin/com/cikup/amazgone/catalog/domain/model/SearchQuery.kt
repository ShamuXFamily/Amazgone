package com.cikup.amazgone.catalog.domain.model

enum class SortOrder { RELEVANCE, PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, TOP_RATED, BIGGEST_DISCOUNT }

data class SearchFilters(
    val categorySlug: String? = null,
    val minPriceCoins: Long? = null,
    val maxPriceCoins: Long? = null,
    val minRating: Double? = null,
    val inStockOnly: Boolean = false,
) {
    val activeCount: Int
        get() = listOfNotNull(categorySlug, minPriceCoins, maxPriceCoins, minRating).size + if (inStockOnly) 1 else 0
}

/** Pure filter + sort applied to local FTS candidates (already in relevance order). */
object ProductQueryEngine {
    fun apply(candidates: List<Product>, filters: SearchFilters, sort: SortOrder): List<Product> =
        candidates.filter { it.matches(filters) }.sortedWith(sort.comparator())

    private fun Product.matches(f: SearchFilters): Boolean =
        (f.categorySlug == null || categorySlug == f.categorySlug) &&
            (f.minPriceCoins == null || priceCoins >= f.minPriceCoins) &&
            (f.maxPriceCoins == null || priceCoins <= f.maxPriceCoins) &&
            (f.minRating == null || (rating ?: 0.0) >= f.minRating) &&
            (!f.inStockOnly || isInStock)

    private fun SortOrder.comparator(): Comparator<Product> = when (this) {
        SortOrder.RELEVANCE -> Comparator { _, _ -> 0 } // stable: keeps FTS order
        SortOrder.PRICE_LOW_TO_HIGH -> compareBy { it.priceCoins }
        SortOrder.PRICE_HIGH_TO_LOW -> compareByDescending { it.priceCoins }
        SortOrder.TOP_RATED -> compareByDescending<Product> { it.rating ?: 0.0 }.thenByDescending { it.ratingCount ?: 0 }
        SortOrder.BIGGEST_DISCOUNT -> compareByDescending { it.discountPercent }
    }

    /**
     * Turns free text into a safe FTS4 prefix query: "iPhone 13" → "iphone* 13*".
     * Returns null when nothing searchable is left.
     */
    fun toFtsQuery(raw: String): String? {
        val tokens = raw.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }
            .take(MAX_TOKENS)
        return if (tokens.isEmpty()) null else tokens.joinToString(" ") { "$it*" }
    }

    private const val MAX_TOKENS = 6
}
