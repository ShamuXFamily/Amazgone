package com.cikup.amazgone.orders.domain.usecase

import com.cikup.amazgone.cart.domain.model.CartSummary
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.orders.domain.model.AddressValidator
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.repository.CourierRepository
import com.cikup.amazgone.orders.domain.model.CheckoutPlanner
import com.cikup.amazgone.orders.domain.model.point
import kotlinx.coroutines.flow.first
import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderDraft
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.progress.domain.model.XpRules
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.wallet.domain.model.Currency
import com.cikup.amazgone.wallet.domain.repository.WalletRepository

/** Works offline: pays from the local ledger now, the server confirms (or refunds) on sync. */
class PlaceOrderUseCase(
    private val orders: OrderRepository,
    private val wallet: WalletRepository,
    private val couriers: CourierRepository,
) {
    suspend operator fun invoke(
        summary: CartSummary,
        address: ShippingAddress,
        courier: Courier = Courier.PIGEON,
    ): DomainResult<Order> {
        if (summary.isEmpty) return DomainResult.Failure(DomainError.NotFound)
        AddressValidator.invalidFields(address).firstOrNull()?.let { field ->
            return DomainResult.Failure(DomainError.Validation(field, ValidationReason.EMPTY))
        }
        if (courier !in couriers.observeOwned().first()) return DomainResult.Failure(DomainError.NotFound)
        if (CheckoutPlanner.courierArrival(CheckoutPlanner.shipments(summary.lines), address.point(), courier, 0) == null) {
            return DomainResult.Failure(DomainError.Validation("courier", ValidationReason.TOO_LONG)) // e.g. drone out of range
        }
        val total = summary.totalCoins // couriers are unlocked once in the Garage, so delivery itself is free
        val balance = wallet.balance(Currency.COINS)
        if (balance < total) {
            return DomainResult.Failure(DomainError.InsufficientCoins(total, balance))
        }
        val draft = OrderDraft(
            items = summary.lines.map {
                OrderItem(
                    productId = it.product.id,
                    title = it.product.title,
                    thumbnailUrl = it.product.thumbnailUrl,
                    quantity = it.quantity,
                    unitPriceCoins = it.product.priceCoins,
                    storeName = it.product.store.name,
                    digital = it.product.store.kind == StoreKind.DIGITAL,
                    storeId = it.product.store.id,
                )
            },
            subtotalCoins = summary.subtotalCoins,
            discountCoins = summary.couponDiscountCoins,
            totalCoins = total,
            couponCode = summary.appliedCoupon?.code,
            address = address.trimmed(),
            delivery = DeliveryOption.STANDARD,
            deliveryFeeCoins = 0,
            courier = courier,
            xpEarned = XpRules.forOrder(summary.totalCoins),
        )
        return orders.placeOrder(draft, summary.appliedCoupon?.code)
    }

    private fun ShippingAddress.trimmed() =
        copy(fullName = fullName.trim(), line1 = line1.trim(), city = city.trim(), postalCode = postalCode.trim(), country = country.trim())
}
