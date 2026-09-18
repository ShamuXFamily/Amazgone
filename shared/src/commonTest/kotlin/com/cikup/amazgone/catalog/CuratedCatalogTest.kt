package com.cikup.amazgone.catalog

import com.cikup.amazgone.catalog.data.mapper.toProductOrNull
import com.cikup.amazgone.catalog.data.remote.CuratedCatalogSource
import com.cikup.amazgone.catalog.data.remote.dto.CuratedProductDto
import com.cikup.amazgone.catalog.data.sync.SeedCatalogImporter
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.remote.DOCS
import com.cikup.amazgone.remote.FakeFirebase
import com.cikup.amazgone.remote.ok
import com.cikup.amazgone.testing.FakeClock
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CuratedCatalogTest {
    private val phone = CuratedProductDto(
        id = "iphone-18-pro",
        title = "iPhone 18 Pro",
        brand = "Apple",
        category = "Smartphones",
        priceUsd = 1199.0,
        images = listOf("https://example.com/a.png", "http://insecure.example.com/b.png"),
        highlights = listOf("A20 Pro", " "),
        releaseDate = "2026-09-18",
        imageCredit = "Photo: someone · CC BY-SA 4.0",
    )

    @Test
    fun mapsIntoAmazgoneProductWithoutInventedRatings() {
        val product = phone.toProductOrNull(now = 5)!!
        assertEquals("amazgone:iphone-18-pro", product.id)
        assertEquals(CatalogSourceId.AMAZGONE, product.source)
        assertEquals("smartphones", product.categorySlug)
        assertEquals(listOf("https://example.com/a.png"), product.imageUrls) // https only
        assertEquals(listOf("A20 Pro"), product.details.highlights)
        assertEquals(1_789_689_600_000L, product.details.releaseDateMillis)
        assertNull(product.rating)
    }

    @Test
    fun rejectsInvalidHandEditedItems() {
        listOf(
            phone.copy(id = "Bad/Id"),
            phone.copy(title = " "),
            phone.copy(priceUsd = 0.0),
            phone.copy(priceUsd = Double.NaN),
            phone.copy(originalPriceUsd = 100.0), // "original" below the price
            phone.copy(published = false),
        ).forEach { assertNull(it.toProductOrNull(0), "should reject $it") }
        assertNull(phone.copy(releaseDate = "soon").toProductOrNull(0)!!.details.releaseDateMillis)
    }

    @Test
    fun parsesBundledFormat() {
        val batch = SeedCatalogImporter.parseNewArrivals(
            """{"products":[{"id":"a","title":"A","category":"smartphones","priceUsd":10},{"id":"B!","title":"B","category":"x","priceUsd":1}]}""",
        )
        assertEquals(listOf("amazgone:a"), batch.products.map { it.id })
    }

    @Test
    fun fetchesPublicCollectionWithoutSignIn() = runTest {
        val backend = FakeFirebase()
        backend.onPathEnds(
            "GET",
            "documents/catalog",
            ok(
                """{"documents":[{"name":"$DOCS/catalog/iphone-duo","fields":{
                "title":{"stringValue":"iPhone Duo"},"category":{"stringValue":"smartphones"},
                "priceUsd":{"integerValue":"1999"},"images":{"arrayValue":{"values":[{"stringValue":"https://x.test/duo.png"}]}},
                "highlights":{"arrayValue":{"values":[{"stringValue":"Foldable"}]}}}},
                {"name":"$DOCS/catalog/broken","fields":{"title":{"stringValue":"No price"}}}]}""",
            ),
        )
        val source = CuratedCatalogSource(FirebaseServices(auth = null, firestore = backend.firestore()), FakeClock(9))

        val batch = source.fetchCatalog()
        assertEquals(listOf("amazgone:iphone-duo"), batch.products.map { it.id })
        assertEquals(1999.0, batch.products.single().priceUsd)
        val request = backend.requests.single()
        assertNull(request.headers[HttpHeaders.Authorization])
        assertEquals("key", request.url.parameters["key"])
    }

    @Test
    fun withoutFirebaseConfigReturnsNothing() = runTest {
        val source = CuratedCatalogSource(FirebaseServices(auth = null, firestore = null), FakeClock(0))
        assertTrue(source.fetchCatalog().products.isEmpty())
    }
}
