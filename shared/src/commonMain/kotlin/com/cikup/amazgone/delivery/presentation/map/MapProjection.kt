package com.cikup.amazgone.delivery.presentation.map

import com.cikup.amazgone.delivery.domain.model.GeoMath
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** Which part of the Web-Mercator world a map shows: zoom + the world-pixel of its top-left corner. */
data class MapFrame(val zoom: Int, val left: Double, val top: Double)

/** Slippy-map (OpenStreetMap tile) maths in "world units": 256 units per tile at each zoom. */
object MapProjection {
    const val TILE = 256.0
    private const val MIN_ZOOM = 1
    private const val MAX_ZOOM = 12
    private const val MAX_LAT = 85.0511
    private const val PADDING = 0.18 // keep pins away from the edges

    fun worldSize(zoom: Int): Double = TILE * 2.0.pow(zoom)

    /** Longitudes outside ±180 are allowed (unwrapped routes); they simply continue east/west. */
    fun worldPoint(p: GeoPoint, zoom: Int): Pair<Double, Double> {
        val size = worldSize(zoom)
        val lat = p.lat.coerceIn(-MAX_LAT, MAX_LAT) * PI / 180
        val x = (p.lon + 180.0) / 360.0 * size
        val y = (1 - ln(tan(lat) + 1 / kotlin.math.cos(lat)) / PI) / 2 * size
        return x to y
    }

    /** Each route unwrapped (no jump at ±180°) and shifted by whole turns so it ends exactly at [home]. */
    fun unwrappedRoutes(pairs: List<Pair<GeoPoint, GeoPoint>>, home: GeoPoint): List<List<GeoPoint>> = pairs.map { (from, to) ->
        val route = GeoMath.route(from, to)
        val shift = home.lon - route.last().lon
        route.map { GeoPoint(it.lat, it.lon + shift) }
    }

    /** Largest zoom at which every point fits a [width]×[height] view (in the same units as tiles are drawn). */
    fun frame(points: List<GeoPoint>, width: Double, height: Double): MapFrame {
        val usableW = width * (1 - 2 * PADDING)
        val usableH = height * (1 - 2 * PADDING)
        var zoom = MAX_ZOOM
        while (zoom > MIN_ZOOM) {
            val (w, h) = span(points, zoom)
            if (w <= usableW && h <= usableH) break
            zoom--
        }
        val world = points.map { worldPoint(it, zoom) }
        val cx = (world.minOf { it.first } + world.maxOf { it.first }) / 2
        val cy = (world.minOf { it.second } + world.maxOf { it.second }) / 2
        return MapFrame(zoom, cx - width / 2, cy - height / 2)
    }

    private fun span(points: List<GeoPoint>, zoom: Int): Pair<Double, Double> {
        val world = points.map { worldPoint(it, zoom) }
        return (world.maxOf { it.first } - world.minOf { it.first }) to (world.maxOf { it.second } - world.minOf { it.second })
    }

    /** Tiles covering the frame; x may leave 0 until 2^z (drawn unwrapped, fetched with x wrapped). */
    fun tiles(frame: MapFrame, width: Double, height: Double): List<Pair<Int, Int>> {
        val count = 2.0.pow(frame.zoom).toInt()
        val x0 = floor(frame.left / TILE).toInt()
        val x1 = floor((frame.left + width) / TILE).toInt()
        val y0 = floor(frame.top / TILE).toInt().coerceAtLeast(0)
        val y1 = floor((frame.top + height) / TILE).toInt().coerceAtMost(count - 1)
        return (y0..y1).flatMap { y -> (x0..x1).map { x -> x to y } }
    }

    fun wrapX(x: Int, zoom: Int): Int {
        val count = 2.0.pow(zoom).toInt()
        return ((x % count) + count) % count
    }
}
