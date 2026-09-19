package com.cikup.amazgone.delivery.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class GeoPoint(val lat: Double, val lon: Double)

/** Spherical-earth maths: good to ~0.5%, plenty for delivery estimates and drawing routes. */
object GeoMath {
    private const val EARTH_RADIUS_KM = 6_371.0
    private const val DEG = kotlin.math.PI / 180.0

    /** Great-circle (haversine) distance. */
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = (b.lat - a.lat) * DEG
        val dLon = (b.lon - a.lon) * DEG
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(a.lat * DEG) * cos(b.lat * DEG) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Point a fraction [t] of the way along the great circle from [a] to [b] (where the courier is). */
    fun interpolate(a: GeoPoint, b: GeoPoint, t: Double): GeoPoint {
        val d = distanceKm(a, b) / EARTH_RADIUS_KM
        if (d < 1e-9) return a
        val sinD = sin(d)
        val f1 = sin((1 - t) * d) / sinD
        val f2 = sin(t * d) / sinD
        val (lat1, lon1, lat2, lon2) = listOf(a.lat * DEG, a.lon * DEG, b.lat * DEG, b.lon * DEG)
        val x = f1 * cos(lat1) * cos(lon1) + f2 * cos(lat2) * cos(lon2)
        val y = f1 * cos(lat1) * sin(lon1) + f2 * cos(lat2) * sin(lon2)
        val z = f1 * sin(lat1) + f2 * sin(lat2)
        return GeoPoint(atan2(z, sqrt(x * x + y * y)) / DEG, atan2(y, x) / DEG)
    }

    /** Points along the route for drawing; longitudes are "unwrapped" so routes across the date line stay continuous. */
    fun route(a: GeoPoint, b: GeoPoint, segments: Int = 64): List<GeoPoint> {
        val points = (0..segments).map { interpolate(a, b, it / segments.toDouble()) }
        return points.fold(mutableListOf()) { acc, p ->
            val prev = acc.lastOrNull()
            var lon = p.lon
            if (prev != null) {
                while (lon - prev.lon > 180) lon -= 360
                while (lon - prev.lon < -180) lon += 360
            }
            acc += GeoPoint(p.lat, lon)
            acc
        }
    }
}
