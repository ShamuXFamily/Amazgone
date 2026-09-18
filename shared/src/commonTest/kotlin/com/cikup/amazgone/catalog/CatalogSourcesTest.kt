package com.cikup.amazgone.catalog

import com.cikup.amazgone.catalog.data.mapper.VIDEO_GAMES_CATEGORY
import com.cikup.amazgone.catalog.data.remote.CheapSharkSource
import com.cikup.amazgone.catalog.data.remote.DummyJsonSource
import com.cikup.amazgone.catalog.data.sync.SeedCatalogImporter
import com.cikup.amazgone.core.network.NetworkConfig
import com.cikup.amazgone.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal const val DUMMY_JSON = """{"products":[{"id":1,"title":"Mascara","description":"Lashes","category":"beauty",
"price":9.0,"discountPercentage":10.0,"rating":4.5,"stock":3,"tags":["beauty"],"brand":"Essence",
"warrantyInformation":"1 week","reviews":[{"rating":5,"comment":"Great","date":"2025-04-30T09:41:02.053Z","reviewerName":"Ana"}],
"images":["https://img/1.webp"],"thumbnail":"https://img/t.webp","unknownField":true},
{"id":2,"title":"Plain","price":5.0,"thumbnail":"https://img/p.webp"}],"total":2}"""

internal const val DEALS_JSON = """[
{"title":"Portal","gameID":"82","steamAppID":"400","salePrice":"4.99","normalPrice":"9.99","steamRatingText":"Overwhelmingly Positive","steamRatingPercent":"98","steamRatingCount":"1000","metacriticScore":"90","releaseDate":1191974400,"thumb":"https://t/portal.jpg"},
{"title":"Portal","gameID":"82","steamAppID":"400","salePrice":"3.99","normalPrice":"9.99","steamRatingPercent":"98","thumb":"https://t/portal.jpg"},
{"title":"Indie","gameID":"7","steamAppID":null,"salePrice":"1.00","normalPrice":"1.00","steamRatingPercent":"0","metacriticScore":"0","thumb":"https://t/indie.jpg"}]"""

class CatalogSourcesTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK) = createHttpClient(
        MockEngine { request ->
            requests += request
            respondJson(body, status)
        },
    )

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun dummyJsonFetchesWholeCatalogAndMapsFields() = runTest {
        val batch = DummyJsonSource(client(DUMMY_JSON), { 7L }).fetchCatalog()

        assertEquals("0", requests.single().url.parameters["limit"])
        val mascara = batch.products.first()
        assertEquals("dummyjson:1", mascara.id)
        assertEquals(10.0, mascara.originalPriceUsd!!, 0.001)
        assertEquals(10, mascara.discountPercent)
        assertEquals("1 week", mascara.details.warranty)
        assertEquals(7L, mascara.updatedAt)
        assertEquals(1, batch.reviews.size)
        assertTrue(batch.reviews.single().dateMillis > 0)
    }

    @Test
    fun missingOptionalFieldsFallBackSafely() = runTest {
        val plain = DummyJsonSource(client(DUMMY_JSON), { 0L }).fetchCatalog().products[1]
        assertNull(plain.originalPriceUsd)
        assertEquals("misc", plain.categorySlug)
        assertEquals(listOf("https://img/p.webp"), plain.imageUrls)
    }

    @Test
    fun cheapSharkSendsUserAgentAndKeepsCheapestDealPerGame() = runTest {
        val batch = CheapSharkSource(client(DEALS_JSON), { 0L }).fetchCatalog()

        assertEquals(NetworkConfig.USER_AGENT, requests.single().headers[HttpHeaders.UserAgent])
        assertEquals(2, batch.products.size)
        val portal = batch.products.first { it.id == "cheapshark:82" }
        assertEquals(3.99, portal.priceUsd, 0.001)
        assertEquals(4.9, portal.rating!!, 0.001)
        assertEquals(VIDEO_GAMES_CATEGORY, portal.categorySlug)
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/400/header.jpg", portal.imageUrls.first())
    }

    @Test
    fun zeroRatingsAndMissingSteamIdAreTreatedAsUnknown() = runTest {
        val indie = CheapSharkSource(client(DEALS_JSON), { 0L }).fetchCatalog().products.first { it.id == "cheapshark:7" }
        assertNull(indie.rating)
        assertNull(indie.originalPriceUsd)
        assertNull(indie.details.metacriticScore)
        assertEquals(listOf("https://t/indie.jpg"), indie.imageUrls)
    }

    @Test
    fun cheapSharkSearchMapsGames() = runTest {
        val json = """[{"gameID":"82","steamAppID":"400","cheapest":"9.99","external":"Portal","thumb":"x"}]"""
        val batch = CheapSharkSource(client(json), { 0L }).search("portal")
        assertEquals("portal", requests.single().url.parameters["title"])
        assertEquals("Portal", batch.products.single().title)
    }

    @Test
    fun httpErrorsSurfaceAsExceptionsForTheSyncerToHandle() = runTest {
        assertFailsWith<Exception> {
            DummyJsonSource(client("{}", HttpStatusCode.InternalServerError), { 0L }).fetchCatalog()
        }
    }

    @Test
    fun seedFileUsesTheSameMappers() {
        val batch = SeedCatalogImporter.parseSeed("""{"dummyjson":$DUMMY_JSON,"cheapshark":$DEALS_JSON}""")
        assertEquals(4, batch.products.size)
        assertTrue(batch.products.all { it.updatedAt == 0L })
    }
}
