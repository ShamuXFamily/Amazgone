package com.cikup.amazgone.delivery.domain.usecase

import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.domain.model.Origins
import com.cikup.amazgone.delivery.domain.repository.CourierRepository
import com.cikup.amazgone.delivery.domain.repository.Geocoder
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import kotlinx.coroutines.flow.Flow

class ObserveOwnedCouriersUseCase(private val couriers: CourierRepository) {
    operator fun invoke(): Flow<Set<Courier>> = couriers.observeOwned()
}

class BuyCourierUseCase(private val couriers: CourierRepository) {
    suspend operator fun invoke(courier: Courier): DomainResult<Unit> = couriers.buy(courier)
}

/**
 * Where the parcel goes: the exact address when OpenStreetMap knows it, else the city, else the
 * country's centre, else the Amazgone warehouse city — so estimates always work, even offline.
 */
class LocateAddressUseCase(private val geocoder: Geocoder) {
    suspend operator fun invoke(address: ShippingAddress): GeoPoint {
        val exact = listOf(address.line1, address.city, address.postalCode, address.country).filter { it.isNotBlank() }
        return geocoder.locate(exact.joinToString(", "))
            ?: geocoder.locate(listOf(address.city, address.country).filter { it.isNotBlank() }.joinToString(", "))
            ?: Origins.countryCentre(address.country)
            ?: Origins.DEFAULT_DESTINATION
    }
}
