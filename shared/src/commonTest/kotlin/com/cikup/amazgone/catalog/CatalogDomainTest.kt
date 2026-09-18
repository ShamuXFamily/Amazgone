package com.cikup.amazgone.catalog

import app.cash.turbine.test
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.CoinPricing
import com.cikup.amazgone.catalog.domain.model.ProductQueryEngine
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.catalog.domain.usecase.ObserveRecommendationsUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchProductsUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchRemoteCatalogUseCase
import com.cikup.amazgone.testing.FakeCatalogRepository
import com.cikup.amazgone.testing.product
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogDomainTest {

    @Test
    fun coinsRoundUpAndNothingIsFree() {
        assertEquals(100L, CoinPricing.toCoins(9.999))
        assertEquals(100L, CoinPricing.toCoins(10.0))
        assertEquals(CoinPricing.MIN_PRICE_COINS, CoinPricing.toCoins(0.0))
        assertEquals(CoinPricing.MIN_PRICE_COINS, CoinPricing.toCoins(-3.0))
    }

    @Test
    fun discountIsZeroWithoutAHigherOriginalPrice() {
        assertEquals(50, CoinPricing.discountPercent(5.0, 10.0))
        assertEquals(0, CoinPricing.discountPercent(10.0, null))
        assertEquals(0, CoinPricing.discountPercent(12.0, 10.0))
    }

    @Test
    fun productIdsCarryTheirSource() {
        assertEquals("cheapshark:42", CatalogSourceId.CHEAP_SHARK.productId("42"))
        assertEquals(CatalogSourceId.DUMMY_JSON, CatalogSourceId.fromProductId("dummyjson:7"))
        assertNull(CatalogSourceId.fromProductId("unknown:7"))
    }

    @Test
    fun ftsQueryIsSanitizedIntoPrefixTokens() {
        assertEquals("iphone* 13*", ProductQueryEngine.toFtsQuery("  iPhone 13!! "))
        // lowercasing neutralises FTS operators (OR/AND/NOT are uppercase-only) and quotes are stripped
        assertEquals("ab* or* cd*", ProductQueryEngine.toFtsQuery("ab\" OR cd*"))
        assertNull(ProductQueryEngine.toFtsQuery(" ?!* "))
    }

    @Test
    fun filtersCombine() {
        val items = listOf(
            product("1", price = 5.0, rating = 4.8),
            product("2", price = 50.0, rating = 4.9),
            product("3", price = 6.0, rating = 2.0),
            product("4", price = 7.0, rating = 4.5, stock = 0),
        )
        val result = ProductQueryEngine.apply(
            items,
            SearchFilters(maxPriceCoins = 100, minRating = 4.0, inStockOnly = true),
            SortOrder.RELEVANCE,
        )
        assertEquals(listOf("dummyjson:1"), result.map { it.id })
    }

    @Test
    fun sortsByPriceRatingAndDiscount() {
        val a = product("a", price = 30.0, rating = 3.0)
        val b = product("b", price = 10.0, rating = 5.0, original = 40.0)
        val c = product("c", price = 20.0, rating = 4.0, original = 25.0)
        val all = listOf(a, b, c)
        fun ids(sort: SortOrder) = ProductQueryEngine.apply(all, SearchFilters(), sort).map { it.id.substringAfter(':') }

        assertEquals(listOf("a", "b", "c"), ids(SortOrder.RELEVANCE))
        assertEquals(listOf("b", "c", "a"), ids(SortOrder.PRICE_LOW_TO_HIGH))
        assertEquals(listOf("a", "c", "b"), ids(SortOrder.PRICE_HIGH_TO_LOW))
        assertEquals(listOf("b", "c", "a"), ids(SortOrder.TOP_RATED))
        assertEquals(listOf("b", "c", "a"), ids(SortOrder.BIGGEST_DISCOUNT))
    }

    @Test
    fun activeFilterCountDrivesTheBadge() {
        assertEquals(0, SearchFilters().activeCount)
        assertEquals(3, SearchFilters(categorySlug = "x", minRating = 4.0, inStockOnly = true).activeCount)
    }

    @Test
    fun emptyQueryBrowsesTheCategory() = runTest {
        val repo = FakeCatalogRepository(listOf(product("1", category = "a"), product("2", category = "b")))
        SearchProductsUseCase(repo)("  ", SearchFilters(categorySlug = "b"), SortOrder.RELEVANCE).test {
            assertEquals(listOf("dummyjson:2"), awaitItem().map { it.id })
        }
    }

    @Test
    fun searchResultsUpdateWhenTheCatalogChanges() = runTest {
        val repo = FakeCatalogRepository(listOf(product("1", title = "Red Phone")))
        SearchProductsUseCase(repo)("phone", SearchFilters(), SortOrder.RELEVANCE).test {
            assertEquals(1, awaitItem().size)
            repo.products.value = repo.products.value + product("2", title = "Blue phone")
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun remoteSearchNeedsThreeCharacters() = runTest {
        val repo = FakeCatalogRepository()
        val useCase = SearchRemoteCatalogUseCase(repo)
        useCase("ab")
        useCase("  portal ")
        assertEquals(listOf("portal"), repo.remoteQueries)
    }

    @Test
    fun recommendationsExcludeSelfAndTopUpFromTopRated() = runTest {
        val self = product("self", category = "rare", rating = 5.0)
        val sibling = product("sib", category = "rare", rating = 3.0)
        val popular = product("pop", category = "other", rating = 4.9)
        val repo = FakeCatalogRepository(listOf(self, sibling, popular))

        ObserveRecommendationsUseCase(repo)(self, limit = 5).test {
            assertEquals(listOf("dummyjson:sib", "dummyjson:pop"), awaitItem().map { it.id })
        }
    }
}
