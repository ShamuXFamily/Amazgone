package com.cikup.amazgone.catalog.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DummyProductsResponse(
    val products: List<DummyProductDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class DummyProductDto(
    val id: Int,
    val title: String,
    val description: String = "",
    val category: String = "misc",
    val price: Double,
    val discountPercentage: Double? = null,
    val rating: Double? = null,
    val stock: Int? = null,
    val tags: List<String> = emptyList(),
    val brand: String? = null,
    val sku: String? = null,
    val warrantyInformation: String? = null,
    val shippingInformation: String? = null,
    val availabilityStatus: String? = null,
    val reviews: List<DummyReviewDto> = emptyList(),
    val returnPolicy: String? = null,
    val minimumOrderQuantity: Int? = null,
    val images: List<String> = emptyList(),
    val thumbnail: String = "",
)

@Serializable
data class DummyReviewDto(
    val rating: Int,
    val comment: String = "",
    val date: String? = null,
    val reviewerName: String = "",
)
