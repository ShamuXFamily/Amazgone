package com.cikup.amazgone.catalog

import app.cash.turbine.test
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.remote.CatalogSource
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.catalog.data.sync.CatalogSyncer
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.ProductQueryEngine
import com.cikup.amazgone.catalog.domain.model.Review
import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.database.RoomTransactionRunner
import com.cikup.amazgone.core.database.inMemoryDatabase
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeSource(var batch: CatalogBatch, var searchBatch: CatalogBatch = CatalogBatch.EMPTY) : CatalogSource {
    override val id = CatalogSourceId.DUMMY_JSON
    override val ttlMillis = 1_000L
    var fetches = 0
    override suspend fun fetchCatalog(): CatalogBatch { fetches++; return batch }
    override suspend fun search(query: String) = searchBatch
}

class CatalogRepositoryTest {
    private val db = inMemoryDatabase()
    private val source = FakeSource(CatalogBatch.EMPTY)
    private val repo = CatalogRepositoryImpl(db.catalogDao(), listOf(source), RoomTransactionRunner(db), PrintLogger)

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun ftsFindsPrefixMatchesAcrossTitleBrandAndTags() = runTest {
        repo.save(
            CatalogBatch(
                listOf(
                    product("1", title = "iPhone 13 Pro"),
                    product("2", title = "Galaxy S21").copy(brand = "Samsung"),
                    product("3", title = "Lamp").copy(tags = listOf("home-decoration")),
                ),
                emptyList(),
            ),
        )
        suspend fun find(q: String) = repo.search(ProductQueryEngine.toFtsQuery(q)!!).first().map { it.title }

        assertEquals(listOf("iPhone 13 Pro"), find("iph"))
        assertEquals(listOf("Galaxy S21"), find("sams"))
        assertEquals(listOf("Lamp"), find("decor"))
    }

    @Test
    fun ftsIndexFollowsUpdates() = runTest {
        repo.save(CatalogBatch(listOf(product("1", title = "Old name")), emptyList()))
        repo.save(CatalogBatch(listOf(product("1", title = "Shiny new")), emptyList()))

        assertEquals(1, repo.search("shiny*").first().size)
        assertEquals(0, repo.search("old*").first().size)
    }

    @Test
    fun dealsAreOrderedByDiscountAndCategoriesAreCounted() = runTest {
        repo.save(
            CatalogBatch(
                listOf(
                    product("a", price = 9.0, original = 10.0, category = "x"),
                    product("b", price = 2.0, original = 10.0, category = "x"),
                    product("c", price = 5.0, category = "y"),
                ),
                emptyList(),
            ),
        )
        assertEquals(listOf("dummyjson:b", "dummyjson:a"), repo.observeDeals(5).first().map { it.id })
        assertEquals(listOf("x" to 2, "y" to 1), repo.observeCategories().first().map { it.slug to it.productCount })
    }

    @Test
    fun productDetailEmitsReviewsAndReplacesThemOnRefresh() = runTest {
        val p = product("1")
        fun review(comment: String) = Review(p.id, "Ana", 5, comment, 0)
        repo.save(CatalogBatch(listOf(p), listOf(review("old"), review("older"))))

        repo.observeProduct(p.id).test {
            assertEquals(2, awaitItem()!!.reviews.size)
            repo.save(CatalogBatch(listOf(p), listOf(review("new"))))
            var latest = awaitItem()!!
            while (latest.reviews.size != 1) latest = awaitItem()!!
            assertEquals("new", latest.reviews.single().comment)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun remoteSearchNeverOverwritesKnownProducts() = runTest {
        repo.save(CatalogBatch(listOf(product("1", rating = 4.9)), emptyList()))
        source.searchBatch = CatalogBatch(listOf(product("1", rating = null), product("2")), emptyList())

        repo.searchRemote("anything")

        val stored = repo.productsByIds(listOf("dummyjson:1", "dummyjson:2"))
        assertEquals(2, stored.size)
        assertEquals(4.9, stored.first { it.id == "dummyjson:1" }.rating)
    }

    @Test
    fun syncerRespectsTtlUnlessForced() = runTest {
        val clock = FakeClock()
        source.batch = CatalogBatch(listOf(product("1")), emptyList())
        val syncer = CatalogSyncer(listOf(source), repo, db.syncMetaDao(), clock, PrintLogger)

        syncer.pull(force = false)
        syncer.pull(force = false)
        assertEquals(1, source.fetches)

        syncer.pull(force = true)
        assertEquals(2, source.fetches)

        clock.advance(source.ttlMillis)
        syncer.pull(force = false)
        assertEquals(3, source.fetches)
        assertTrue(!repo.isEmpty())
    }

    @Test
    fun failedRefreshKeepsTheCachedCatalog() = runTest {
        repo.save(CatalogBatch(listOf(product("cached")), emptyList()))
        val broken = object : CatalogSource {
            override val id = CatalogSourceId.CHEAP_SHARK
            override val ttlMillis = 0L
            override suspend fun fetchCatalog(): CatalogBatch = error("offline")
            override suspend fun search(query: String) = CatalogBatch.EMPTY
        }
        CatalogSyncer(listOf(broken), repo, db.syncMetaDao(), FakeClock(), PrintLogger).pull(force = true)

        assertEquals(1, repo.observeProducts(null).first().size)
    }
}
