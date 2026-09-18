package com.cikup.amazgone.catalog.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One hand-curated product. Same shape in the bundled `files/seed/new_arrivals.json` and in the
 * Firestore `catalog/{id}` documents (see tools/catalog/README.md).
 */
@Serializable
data class CuratedProductDto(
    val id: String,
    val title: String,
    val brand: String? = null,
    val category: String,
    val description: String = "",
    val priceUsd: Double,
    val originalPriceUsd: Double? = null,
    val stock: Int? = null,
    val availability: String? = null,
    val images: List<String> = emptyList(),
    val imageCredit: String? = null,
    val highlights: List<String> = emptyList(),
    /** ISO date, e.g. "2026-09-18". */
    val releaseDate: String? = null,
    val warranty: String? = null,
    val shipping: String? = null,
    val returnPolicy: String? = null,
    val tags: List<String> = emptyList(),
    /** Lets an editor hide a product without deleting the document. */
    val published: Boolean = true,
)

@Serializable
data class CuratedCatalogDto(val products: List<CuratedProductDto>)
