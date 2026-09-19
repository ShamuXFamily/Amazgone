package com.cikup.amazgone.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/** CheapShark returns most numbers as strings. */
@Serializable
data class CheapSharkDealDto(
    val title: String,
    val gameID: String,
    val dealID: String? = null,
    val storeID: String? = null,
    val steamAppID: String? = null,
    val salePrice: String,
    val normalPrice: String? = null,
    val steamRatingText: String? = null,
    val steamRatingPercent: String? = null,
    val steamRatingCount: String? = null,
    val metacriticScore: String? = null,
    val releaseDate: Long? = null,
    val thumb: String = "",
)

@Serializable
data class CheapSharkGameDto(
    val gameID: String,
    val steamAppID: String? = null,
    val cheapest: String,
    val external: String,
    val thumb: String = "",
)

/** Layout of the bundled seed file (raw API responses). */
@Serializable
data class SeedCatalogDto(
    val dummyjson: DummyProductsResponse,
    val cheapshark: List<CheapSharkDealDto> = emptyList(),
)
