package com.cikup.amazgone.delivery.domain.repository

import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface CourierRepository {
    /** Starters plus every courier bought (pending purchases included, like pending orders). */
    fun observeOwned(): Flow<Set<Courier>>

    /** Bought couriers with their purchase time (starters excluded). */
    fun observePurchases(): Flow<Map<Courier, Long>>

    /** One local transaction: re-checks the balance, debits the price (pending), unlocks, queues the server push. */
    suspend fun buy(courier: Courier): DomainResult<Unit>
}

/** Turns a postal address into coordinates (OpenStreetMap Nominatim); null when unknown or offline. */
interface Geocoder {
    suspend fun locate(query: String): GeoPoint?
}
