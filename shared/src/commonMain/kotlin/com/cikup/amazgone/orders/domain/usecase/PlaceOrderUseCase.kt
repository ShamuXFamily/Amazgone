package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.orders.domain.model.AddressValidator
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderDraft
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.progress.domain.model.XpRules
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.repository.WalletRepository

/** Works offline: pays from the local ledger now, the server confirms (or refunds) on sync. */
class PlaceOrderUseCase(
    private val orders: OrderRepository,
    private val wallet: WalletRepository,
) {
    suspend operator fun invoke(summary: CartSummary, address: ShippingAddress): DomainResult<Order> {
        if (summary.isEmpty) return DomainResult.Failure(DomainError.NotFound)
        AddressValidator.invalidFields(address).firstOrNull()?.let { field ->
            return DomainResult.Failure(DomainError.Validation(field, ValidationReason.EMPTY))
        }
        val balance = wallet.balance(Currency.COINS)
        if (balance < summary.totalCoins) {
            return DomainResult.Failure(DomainError.InsufficientCoins(summary.totalCoins, balance))
        }
        val draft = OrderDraft(
            items = summary.lines.map { OrderItem(it.product.id, it.product.title, it.product.thumbnailUrl, it.quantity, it.product.priceCoins) },
            subtotalCoins = summary.subtotalCoins,
            discountCoins = summary.couponDiscountCoins,
            totalCoins = summary.totalCoins,
            couponCode = summary.appliedCoupon?.code,
            address = address.trimmed(),
            xpEarned = XpRules.forOrder(summary.totalCoins),
        )
        return orders.placeOrder(draft, summary.appliedCoupon?.code)
    }

    private fun ShippingAddress.trimmed() =
        ShippingAddress(fullName.trim(), line1.trim(), city.trim(), postalCode.trim(), country.trim())
}
