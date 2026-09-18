package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.repository.CouponRepository
import com.cikup.amazgone.core.common.TimeProvider
import kotlinx.coroutines.flow.Flow

class ObserveCouponsUseCase(private val coupons: CouponRepository, private val time: TimeProvider) {
    operator fun invoke(): Flow<List<Coupon>> = coupons.observeAvailable(time.nowMillis())
}
