package com.cikup.amazgone.catalog.data.remote

import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.mapper.toBatch
import com.cikup.amazgone.catalog.data.mapper.toProduct
import com.cikup.amazgone.catalog.data.remote.dto.CheapSharkDealDto
import com.cikup.amazgone.catalog.data.remote.dto.CheapSharkGameDto
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.core.common.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Live video game deals. Requires the descriptive User-Agent set in createHttpClient. */
class CheapSharkSource(
    private val client: HttpClient,
    private val time: TimeProvider,
    private val baseUrl: String = BASE_URL,
) : CatalogSource {
    override val id = CatalogSourceId.CHEAP_SHARK
    override val ttlMillis = 60 * 60 * 1_000L

    override suspend fun fetchCatalog(): CatalogBatch =
        client.get("$baseUrl/deals") {
            parameter("pageSize", PAGE_SIZE)
            parameter("sortBy", "Deal Rating")
            parameter("onSale", 1)
            parameter("steamRating", MIN_STEAM_RATING)
        }.body<List<CheapSharkDealDto>>().toBatch(time.nowMillis())

    override suspend fun search(query: String): CatalogBatch {
        val now = time.nowMillis()
        val games = client.get("$baseUrl/games") {
            parameter("title", query)
            parameter("limit", SEARCH_LIMIT)
        }.body<List<CheapSharkGameDto>>()
        return CatalogBatch(games.map { it.toProduct(now) }, emptyList())
    }

    private companion object {
        const val BASE_URL = "https://www.cheapshark.com/api/1.0"
        const val PAGE_SIZE = 60
        const val MIN_STEAM_RATING = 75
        const val SEARCH_LIMIT = 20
    }
}
