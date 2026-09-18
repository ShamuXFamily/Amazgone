package com.cikup.amazgone.cart.domain.model

/** Pure pricing rules for the cart. All amounts are coins. */
object CartCalculator {
    const val MAX_QUANTITY_PER_ITEM = 10
    private const val PERCENT = 100

    fun summarize(lines: List<CartLine>, coupon: Coupon? = null, now: Long = 0L): CartSummary {
        if (lines.isEmpty()) return CartSummary.EMPTY
        val subtotal = lines.sumOf { it.lineTotalCoins }
        val usableCoupon = coupon?.takeIf { isUsable(it, subtotal, now) }
        val discount = usableCoupon?.let { applyCoupon(subtotal, it) } ?: 0L
        return CartSummary(
            lines = lines,
            itemCount = lines.sumOf { it.quantity },
            subtotalCoins = subtotal,
            savingsCoins = lines.sumOf { it.lineSavingsCoins },
            couponDiscountCoins = discount,
            totalCoins = subtotal - discount,
            appliedCoupon = usableCoupon,
        )
    }

    fun isUsable(coupon: Coupon, subtotal: Long, now: Long): Boolean =
        subtotal >= coupon.minSubtotalCoins && (coupon.expiresAt == null || now < coupon.expiresAt)

    /**
     * Discount in coins that [coupon] takes off [subtotal].
     * Percent coupons are rounded down (the shop never gives away a fractional coin);
     * no coupon can make an order free or negative — at least 1 coin is always paid.
     */
    fun applyCoupon(subtotal: Long, coupon: Coupon): Long {
        if (subtotal <= 0) return 0
        val raw = when (coupon.kind) {
            CouponKind.PERCENT -> subtotal * coupon.value.coerceIn(0, PERCENT.toLong()) / PERCENT
            CouponKind.FLAT_COINS -> coupon.value.coerceAtLeast(0)
        }
        return raw.coerceAtMost(subtotal - 1)
    }

    fun clampQuantity(quantity: Int): Int = quantity.coerceIn(0, MAX_QUANTITY_PER_ITEM)
}
