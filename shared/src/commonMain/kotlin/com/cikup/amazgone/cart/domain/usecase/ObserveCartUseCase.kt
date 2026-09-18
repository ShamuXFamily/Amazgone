package com.cikup.amazgone.cart.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.repository.CartRepository
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import com.cikup.amazgone.core.common.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

/** Cart entries joined with their products and priced; products missing from the catalog are skipped. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCartUseCase(
    private val cart: CartRepository,
    private val catalog: CatalogRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(coupon: Flow<Coupon?> = flowOf(null)): Flow<CartSummary> {
        val lines = cart.observeEntries().mapLatest { entries ->
            val products = catalog.productsByIds(entries.map { it.productId }).associateBy { it.id }
            entries.mapNotNull { entry -> products[entry.productId]?.let { CartLine(it, entry.quantity) } }
        }
        return combine(lines, coupon) { current, applied -> CartCalculator.summarize(current, applied, time.nowMillis()) }
    }
}
