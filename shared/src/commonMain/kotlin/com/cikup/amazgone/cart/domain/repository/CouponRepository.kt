package com.cikup.amazgone.cart.domain.repository

import com.cikup.amazgone.cart.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

/** Coupons are won in mini-games and live on the device only. */
interface CouponRepository {
    fun observeAvailable(now: Long): Flow<List<Coupon>>
    suspend fun grant(coupon: Coupon)
}
