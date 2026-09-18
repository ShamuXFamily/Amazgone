package com.cikup.amazgone.cart.domain.model

import com.cikup.amazgone.catalog.domain.model.Product

/** Raw cart row as stored locally (quantity 0 = removed tombstone, hidden from the UI). */
data class CartEntry(val productId: String, val quantity: Int, val updatedAt: Long)

data class CartLine(val product: Product, val quantity: Int) {
    val lineTotalCoins: Long get() = product.priceCoins * quantity
    val lineSavingsCoins: Long get() = ((product.originalPriceCoins ?: product.priceCoins) - product.priceCoins) * quantity
}

enum class CouponKind { PERCENT, FLAT_COINS }

/** Won in mini-games; applied at checkout. */
data class Coupon(
    val code: String,
    val kind: CouponKind,
    val value: Long,
    val minSubtotalCoins: Long = 0,
    val expiresAt: Long? = null,
)

data class CartSummary(
    val lines: List<CartLine>,
    val itemCount: Int,
    val subtotalCoins: Long,
    val savingsCoins: Long,
    val couponDiscountCoins: Long,
    val totalCoins: Long,
    val appliedCoupon: Coupon?,
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    companion object {
        val EMPTY = CartSummary(emptyList(), 0, 0, 0, 0, 0, null)
    }
}
