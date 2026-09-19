package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.orders.domain.model.AddressValidator
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.orders.domain.model.DeliveryOption
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
    suspend operator fun invoke(
        summary: CartSummary,
        address: ShippingAddress,
        delivery: DeliveryOption = DeliveryOption.STANDARD,
    ): DomainResult<Order> {
        if (summary.isEmpty) return DomainResult.Failure(DomainError.NotFound)
        AddressValidator.invalidFields(address).firstOrNull()?.let { field ->
            return DomainResult.Failure(DomainError.Validation(field, ValidationReason.EMPTY))
        }
        val fee = CheckoutPlanner.deliveryFee(CheckoutPlanner.shipments(summary.lines), delivery)
        val total = summary.totalCoins + fee
        val balance = wallet.balance(Currency.COINS)
        if (balance < total) {
            return DomainResult.Failure(DomainError.InsufficientCoins(total, balance))
        }
        val draft = OrderDraft(
            items = summary.lines.map {
                OrderItem(it.product.id, it.product.title, it.product.thumbnailUrl, it.quantity, it.product.priceCoins, it.product.store.name)
            },
            subtotalCoins = summary.subtotalCoins,
            discountCoins = summary.couponDiscountCoins,
            totalCoins = total,
            couponCode = summary.appliedCoupon?.code,
            address = address.trimmed(),
            delivery = delivery,
            deliveryFeeCoins = fee,
            xpEarned = XpRules.forOrder(summary.totalCoins),
        )
        return orders.placeOrder(draft, summary.appliedCoupon?.code)
    }

    private fun ShippingAddress.trimmed() =
        ShippingAddress(fullName.trim(), line1.trim(), city.trim(), postalCode.trim(), country.trim())
}
