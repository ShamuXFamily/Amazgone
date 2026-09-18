package com.cikup.amazgone.games.domain.usecase

import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.cart.domain.repository.CouponRepository
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.games.domain.model.LightningDeal

/** Grants the window's coupon (valid until the window ends) and puts the product in the cart. */
class ClaimLightningDealUseCase(
    private val coupons: CouponRepository,
    private val addToCart: AddToCartUseCase,
) {
    suspend operator fun invoke(deal: LightningDeal): DomainResult<Int> {
        coupons.grant(
            Coupon(
                code = deal.couponCode,
                kind = CouponKind.PERCENT,
                value = deal.extraPercent,
                minSubtotalCoins = deal.product.priceCoins,
                expiresAt = deal.windowEnd,
            ),
        )
        return addToCart(deal.product.id)
    }
}
