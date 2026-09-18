package com.cikup.amazgone.orders.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ShippingAddress(
    val fullName: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val country: String,
)

@Serializable
data class OrderItem(
    val productId: String,
    val title: String,
    val thumbnailUrl: String,
    val quantity: Int,
    val unitPriceCoins: Long,
)

/** PENDING_SYNC: paid locally, waiting for the server to confirm the coins. */
enum class OrderStatus { PENDING_SYNC, CONFIRMED, REJECTED }

data class Order(
    val id: String,
    val items: List<OrderItem>,
    val subtotalCoins: Long,
    val discountCoins: Long,
    val totalCoins: Long,
    val couponCode: String?,
    val address: ShippingAddress,
    val status: OrderStatus,
    val xpEarned: Long,
    val createdAt: Long,
    val rejectionReason: String?,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
}

/** What checkout hands to the repository; ids and timestamps are assigned there. */
data class OrderDraft(
    val items: List<OrderItem>,
    val subtotalCoins: Long,
    val discountCoins: Long,
    val totalCoins: Long,
    val couponCode: String?,
    val address: ShippingAddress,
    val xpEarned: Long,
)

object AddressValidator {
    const val FIELD_NAME = "fullName"
    const val FIELD_LINE1 = "line1"
    const val FIELD_CITY = "city"
    const val FIELD_POSTAL = "postalCode"
    const val FIELD_COUNTRY = "country"
    private const val MAX_LENGTH = 80
    private val POSTAL = Regex("^[A-Za-z0-9 -]{3,10}$")

    /** Returns the names of invalid fields (empty = valid). */
    fun invalidFields(address: ShippingAddress): Set<String> = buildSet {
        if (address.fullName.isBlankOrTooLong()) add(FIELD_NAME)
        if (address.line1.isBlankOrTooLong()) add(FIELD_LINE1)
        if (address.city.isBlankOrTooLong()) add(FIELD_CITY)
        if (!POSTAL.matches(address.postalCode.trim())) add(FIELD_POSTAL)
        if (address.country.isBlankOrTooLong()) add(FIELD_COUNTRY)
    }

    private fun String.isBlankOrTooLong() = isBlank() || length > MAX_LENGTH
}
