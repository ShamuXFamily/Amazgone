package com.cikup.amazgone.delivery

import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.presentation.map.MapProjection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapProjectionTest {
    private val jakarta = GeoPoint(-6.2088, 106.8456)
    private val cupertino = GeoPoint(37.3349, -122.0090)

    @Test
    fun mercatorPutsNullIslandInTheMiddleOfTheWorld() {
        val z0 = MapProjection.worldPoint(GeoPoint(0.0, 0.0), zoom = 0)
        assertEquals(128.0, z0.first, 1e-9)
        assertEquals(128.0, z0.second, 1e-9)
    }

    @Test
    fun routesAcrossTheDateLineStayContinuousAndEndAtHome() {
        val routes = MapProjection.unwrappedRoutes(listOf(cupertino to jakarta), home = jakarta)
        val route = routes.single()
        assertTrue(route.zipWithNext().all { (a, b) -> abs(a.lon - b.lon) < 30 }, "no jump across the map")
        assertEquals(jakarta.lon, route.last().lon, 1e-6)
    }

    @Test
    fun frameFitsLongTripsZoomedOutAndLocalTripsZoomedIn() {
        val far = MapProjection.frame(MapProjection.unwrappedRoutes(listOf(cupertino to jakarta), jakarta).flatten(), 360.0, 200.0)
        val near = MapProjection.frame(listOf(GeoPoint(-6.20, 106.84), GeoPoint(-6.25, 106.90)), 360.0, 200.0)
        assertTrue(far.zoom <= 2, "far zoom ${far.zoom}")
        assertTrue(near.zoom >= 10, "near zoom ${near.zoom}")
    }
}
