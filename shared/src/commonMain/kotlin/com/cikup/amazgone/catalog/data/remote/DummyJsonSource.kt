package com.cikup.amazgone.catalog.data.remote

import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.mapper.toBatch
import com.cikup.amazgone.catalog.data.remote.dto.DummyProductsResponse
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.core.common.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class DummyJsonSource(
    private val client: HttpClient,
    private val time: TimeProvider,
    private val baseUrl: String = BASE_URL,
) : CatalogSource {
    override val id = CatalogSourceId.DUMMY_JSON
    override val ttlMillis = 24 * 60 * 60 * 1_000L

    /** `limit=0` returns the whole catalog (~200 items) in one call. */
    override suspend fun fetchCatalog(): CatalogBatch =
        client.get("$baseUrl/products") { parameter("limit", 0) }
            .body<DummyProductsResponse>().products.toBatch(time.nowMillis())

    override suspend fun search(query: String): CatalogBatch =
        client.get("$baseUrl/products/search") {
            parameter("q", query)
            parameter("limit", SEARCH_LIMIT)
        }.body<DummyProductsResponse>().products.toBatch(time.nowMillis())

    private companion object {
        const val BASE_URL = "https://dummyjson.com"
        const val SEARCH_LIMIT = 50
    }
}
