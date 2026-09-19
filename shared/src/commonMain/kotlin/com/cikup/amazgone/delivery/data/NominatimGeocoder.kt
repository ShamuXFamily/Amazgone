package com.cikup.amazgone.delivery.data

import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.delivery.domain.model.GeoPoint
import com.cikup.amazgone.delivery.domain.repository.Geocoder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
private data class NominatimPlace(val lat: String, val lon: String)

/**
 * OpenStreetMap's free geocoder. Called once per confirmed address (well inside the 1 request/second
 * policy); the app's User-Agent identifies it as required. Failures return null so callers fall back.
 */
class NominatimGeocoder(
    private val client: HttpClient,
    private val logger: AppLogger,
    private val baseUrl: String = "https://nominatim.openstreetmap.org",
) : Geocoder {
    override suspend fun locate(query: String): GeoPoint? {
        if (query.isBlank()) return null
        return try {
            client.get("$baseUrl/search") {
                parameter("q", query)
                parameter("format", "jsonv2")
                parameter("limit", 1)
            }.body<List<NominatimPlace>>().firstOrNull()?.let { place ->
                val lat = place.lat.toDoubleOrNull() ?: return null
                val lon = place.lon.toDoubleOrNull() ?: return null
                GeoPoint(lat, lon).takeIf { lat in -90.0..90.0 && lon in -180.0..180.0 }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logger.error(TAG, "Geocoding failed; falling back to a rougher location", t)
            null
        }
    }

    private companion object {
        const val TAG = "Geocoder"
    }
}
