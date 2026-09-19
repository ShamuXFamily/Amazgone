package com.cikup.amazgone.delivery.domain.model

data class Origin(val city: String, val country: String, val point: GeoPoint)

/** Where each store ships from (brand headquarters), plus country centres for offline address fallback. */
object Origins {
    private val JAKARTA = Origin("Jakarta", "Indonesia", GeoPoint(-6.2088, 106.8456))
    private val SHENZHEN_HUB = Origin("Shenzhen", "China", GeoPoint(22.5431, 114.0579))

    private val BRAND_HQ: Map<String, Origin> = mapOf(
        "apple" to Origin("Cupertino", "USA", GeoPoint(37.3349, -122.0090)),
        "samsung" to Origin("Suwon", "South Korea", GeoPoint(37.2636, 127.0286)),
        "google" to Origin("Mountain View", "USA", GeoPoint(37.4220, -122.0841)),
        "oppo" to Origin("Dongguan", "China", GeoPoint(23.0207, 113.7518)),
        "vivo" to Origin("Dongguan", "China", GeoPoint(23.0207, 113.7518)),
        "realme" to Origin("Shenzhen", "China", GeoPoint(22.5431, 114.0579)),
        "huawei" to Origin("Shenzhen", "China", GeoPoint(22.6531, 114.0579)),
        "asus" to Origin("Taipei", "Taiwan", GeoPoint(25.0330, 121.5654)),
        "gigabyte" to Origin("New Taipei", "Taiwan", GeoPoint(25.0120, 121.4650)),
        "lenovo" to Origin("Beijing", "China", GeoPoint(39.9042, 116.4074)),
        "dell" to Origin("Round Rock", "USA", GeoPoint(30.5083, -97.6789)),
        "beats" to Origin("Culver City", "USA", GeoPoint(34.0211, -118.3965)),
        "amazon" to Origin("Seattle", "USA", GeoPoint(47.6062, -122.3321)),
        "rolex" to Origin("Geneva", "Switzerland", GeoPoint(46.2044, 6.1432)),
        "longines" to Origin("Saint-Imier", "Switzerland", GeoPoint(47.1525, 6.9960)),
        "iwc" to Origin("Schaffhausen", "Switzerland", GeoPoint(47.6970, 8.6340)),
        "nike" to Origin("Beaverton", "USA", GeoPoint(45.4871, -122.8037)),
        "puma" to Origin("Herzogenaurach", "Germany", GeoPoint(49.5700, 10.8830)),
        "calvin-klein" to Origin("New York", "USA", GeoPoint(40.7128, -74.0060)),
        "off-white" to Origin("Milan", "Italy", GeoPoint(45.4642, 9.1900)),
        "chanel" to Origin("Paris", "France", GeoPoint(48.8566, 2.3522)),
        "dior" to Origin("Paris", "France", GeoPoint(48.8566, 2.3522)),
        "dolce-gabbana" to Origin("Milan", "Italy", GeoPoint(45.4642, 9.1900)),
        "gucci" to Origin("Florence", "Italy", GeoPoint(43.7696, 11.2558)),
        "prada" to Origin("Milan", "Italy", GeoPoint(45.4642, 9.1900)),
        "essence" to Origin("Sulzbach", "Germany", GeoPoint(49.2980, 7.0610)),
        "olay" to Origin("Cincinnati", "USA", GeoPoint(39.1031, -84.5120)),
        "vaseline" to Origin("London", "UK", GeoPoint(51.5072, -0.1276)),
        "knoll" to Origin("East Greenville", "USA", GeoPoint(40.4065, -75.5049)),
        "annibale-colombo" to Origin("Meda", "Italy", GeoPoint(45.6611, 9.1556)),
        "dodge" to Origin("Auburn Hills", "USA", GeoPoint(42.6875, -83.2341)),
        "chrysler" to Origin("Auburn Hills", "USA", GeoPoint(42.6875, -83.2341)),
        "kawasaki" to Origin("Kobe", "Japan", GeoPoint(34.6901, 135.1955)),
    )

    /** Null for digital stores: a game code doesn't travel. */
    fun forStore(storeId: String): Origin? = when {
        storeId.startsWith("digital-") -> null
        storeId.startsWith("official-") -> BRAND_HQ[storeId.removePrefix("official-")] ?: SHENZHEN_HUB
        storeId.startsWith("brand-") -> SHENZHEN_HUB // made-up marketplace brands ship from the hub
        else -> JAKARTA // Amazgone and Amazgone Fresh warehouses
    }

    private val COUNTRY_CENTRES: Map<String, GeoPoint> = mapOf(
        "indonesia" to GeoPoint(-6.2088, 106.8456), "singapore" to GeoPoint(1.3521, 103.8198),
        "malaysia" to GeoPoint(3.1390, 101.6869), "thailand" to GeoPoint(13.7563, 100.5018),
        "philippines" to GeoPoint(14.5995, 120.9842), "vietnam" to GeoPoint(21.0278, 105.8342),
        "japan" to GeoPoint(35.6762, 139.6503), "south korea" to GeoPoint(37.5665, 126.9780),
        "korea" to GeoPoint(37.5665, 126.9780), "china" to GeoPoint(39.9042, 116.4074),
        "india" to GeoPoint(28.6139, 77.2090), "australia" to GeoPoint(-33.8688, 151.2093),
        "usa" to GeoPoint(39.8283, -98.5795), "united states" to GeoPoint(39.8283, -98.5795),
        "us" to GeoPoint(39.8283, -98.5795), "canada" to GeoPoint(43.6532, -79.3832),
        "uk" to GeoPoint(51.5072, -0.1276), "united kingdom" to GeoPoint(51.5072, -0.1276),
        "germany" to GeoPoint(52.5200, 13.4050), "france" to GeoPoint(48.8566, 2.3522),
        "netherlands" to GeoPoint(52.3676, 4.9041), "italy" to GeoPoint(41.9028, 12.4964),
        "spain" to GeoPoint(40.4168, -3.7038), "brazil" to GeoPoint(-23.5505, -46.6333),
        "mexico" to GeoPoint(19.4326, -99.1332), "uae" to GeoPoint(25.2048, 55.2708),
        "saudi arabia" to GeoPoint(24.7136, 46.6753), "egypt" to GeoPoint(30.0444, 31.2357),
        "nigeria" to GeoPoint(6.5244, 3.3792), "south africa" to GeoPoint(-26.2041, 28.0473),
    )

    /** Rough destination when the exact address can't be looked up (offline or unknown). */
    fun countryCentre(country: String): GeoPoint? = COUNTRY_CENTRES[country.trim().lowercase()]

    /** Last resort: the Amazgone warehouse city. */
    val DEFAULT_DESTINATION: GeoPoint = JAKARTA.point
}
