package com.cikup.amazgone.delivery

import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.domain.model.CourierPlan
import com.cikup.amazgone.delivery.domain.model.GeoMath
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.domain.model.Origins
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeliveryDomainTest {
    private val jakarta = GeoPoint(-6.2088, 106.8456)
    private val cupertino = GeoPoint(37.3349, -122.0090)
    private val hour = 60 * 60 * 1_000L
    private val day = 24 * hour

    @Test
    fun greatCircleDistanceMatchesRealWorld() {
        val km = GeoMath.distanceKm(cupertino, jakarta)
        assertTrue(km in 13_800.0..14_300.0, "Cupertino→Jakarta is ~14,000 km, got $km")
        assertEquals(0.0, GeoMath.distanceKm(jakarta, jakarta), 1e-6)
    }

    @Test
    fun interpolationStartsAndEndsAtTheEndpoints() {
        val start = GeoMath.interpolate(cupertino, jakarta, 0.0)
        val end = GeoMath.interpolate(cupertino, jakarta, 1.0)
        assertTrue(abs(start.lat - cupertino.lat) < 1e-6 && abs(end.lon - jakarta.lon) < 1e-6)
    }

    @Test
    fun slowCouriersTakeLongerThanFastOnes() {
        val pigeon = CourierPlan.travelMillis(Courier.PIGEON, 14_000.0)!!
        val jet = CourierPlan.travelMillis(Courier.CARGO_JET, 14_000.0)!!
        assertTrue(pigeon > 20 * day, "pigeon should take weeks")
        assertTrue(jet < 2 * day)
        assertEquals(Courier.TELEPORTER.flatMillis, CourierPlan.travelMillis(Courier.TELEPORTER, 14_000.0))
        assertEquals(60_000L, CourierPlan.arrivalAt(0, Courier.TELEPORTER, 14_000.0)) // no packing wait in the future
    }

    @Test
    fun rangeLimitedCouriersRefuseLongTrips() {
        assertNull(CourierPlan.travelMillis(Courier.DRONE, 5_000.0))
        assertNotNull(CourierPlan.travelMillis(Courier.DRONE, 20.0))
    }

    @Test
    fun progressIsZeroWhileHandlingAndOneOnArrival() {
        val eta = CourierPlan.arrivalAt(0, Courier.CARGO_JET, 14_000.0)!!
        val departs = Courier.CARGO_JET.handlingMillis
        assertEquals(0.0, CourierPlan.progress(departs, eta, departs / 2))
        assertEquals(1.0, CourierPlan.progress(departs, eta, eta + 1))
        val mid = CourierPlan.progress(departs, eta, (departs + eta) / 2)
        assertTrue(mid in 0.45..0.55)
    }

    @Test
    fun storesShipFromTheirHomeCity() {
        assertEquals("Cupertino", Origins.forStore("official-apple")?.city)
        assertEquals("Jakarta", Origins.forStore("amazgone")?.city)
        assertNotNull(Origins.forStore("brand-urban-chic")) // made-up brands use the marketplace hub
        assertNull(Origins.forStore("digital-1")) // codes don't travel
        assertEquals("Indonesia", Origins.countryCentre("indonesia")?.let { "Indonesia" })
    }

    @Test
    fun everyoneStartsWithFreeCouriers() {
        assertTrue(Courier.entries.filter { it.priceCoins == 0L }.map { it.name }.containsAll(listOf("PIGEON", "CAMEL")))
        assertEquals(Courier.entries.sortedBy { it.year }, Courier.entries.toList())
    }
}
