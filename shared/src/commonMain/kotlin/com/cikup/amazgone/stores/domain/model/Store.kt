package com.cikup.amazgone.stores.domain.model

import com.cikup.amazgone.catalog.domain.model.CatalogSourceId

/** Who sells a product, Amazon-style. */
enum class StoreKind {
    /** The manufacturer's own verified store ("Apple Official"). */
    OFFICIAL,
    /** A brand without the verified badge (the demo catalog's made-up brands). */
    BRAND,
    /** A real digital game shop from CheapShark (Steam, GOG, …). */
    DIGITAL,
    /** First party: "Ships from and sold by Amazgone". */
    AMAZGONE,
}

data class Store(val id: String, val name: String, val kind: StoreKind) {
    val isVerified: Boolean get() = kind == StoreKind.OFFICIAL || kind == StoreKind.AMAZGONE
}

/** A store plus what its shelf looks like right now (derived from the local catalog). */
data class StoreSummary(val store: Store, val productCount: Int, val averageRating: Double?, val topCategory: String?)

/**
 * Pure rules that assign every product exactly one store, so the whole feature works offline from
 * catalog data alone. Ids are stable: they are persisted with products and used in routes.
 */
object StoreDirectory {
    val AMAZGONE = Store("amazgone", "Amazgone", StoreKind.AMAZGONE)
    val AMAZGONE_FRESH = Store("amazgone-fresh", "Amazgone Fresh", StoreKind.AMAZGONE)

    private const val OFFICIAL_PREFIX = "official-"
    private const val BRAND_PREFIX = "brand-"
    private const val DIGITAL_PREFIX = "digital-"
    private const val GROCERIES = "groceries"

    /** Real manufacturers in the catalog; anything else with a brand gets a plain brand store. */
    private val OFFICIAL_BRANDS = setOf(
        "apple", "samsung", "google", "oppo", "realme", "vivo", "huawei", "asus", "lenovo", "dell", "gigabyte",
        "beats", "amazon", "rolex", "longines", "iwc", "nike", "puma", "calvin klein", "off white", "chanel", "dior",
        "dolce & gabbana", "gucci", "prada", "essence", "olay", "vaseline", "knoll", "annibale colombo", "dodge",
        "chrysler", "kawasaki", "sony", "microsoft", "nintendo", "xiaomi", "oneplus", "nothing", "bose", "jbl", "adidas",
        "dyson", "canon", "logitech", "dji", "garmin",
    )

    /** Active CheapShark stores (GET /stores). */
    private val DIGITAL_STORES = mapOf(
        "1" to "Steam", "2" to "GamersGate", "3" to "Green Man Gaming", "7" to "GOG", "11" to "Humble Store",
        "13" to "Ubisoft Store", "15" to "Fanatical", "21" to "WinGameStore", "23" to "GameBillet",
        "25" to "Epic Games Store", "27" to "Gamesplanet", "28" to "Gamesload", "30" to "IndieGala", "35" to "DreamGame",
    )

    fun resolve(source: CatalogSourceId, brand: String?, categorySlug: String, sellerId: String?): Store {
        if (source == CatalogSourceId.CHEAP_SHARK) return sellerId?.let(::digital) ?: AMAZGONE
        val name = brand?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            name == null && categorySlug == GROCERIES -> AMAZGONE_FRESH
            name == null -> AMAZGONE
            name.lowercase() in OFFICIAL_BRANDS -> Store(OFFICIAL_PREFIX + slug(name), "$name Official", StoreKind.OFFICIAL)
            else -> Store(BRAND_PREFIX + slug(name), name, StoreKind.BRAND)
        }
    }

    /** Rebuilds a store from a stored id + name (e.g. on an order line). */
    fun fromIdAndName(id: String, name: String): Store {
        val kind = when {
            id.startsWith(OFFICIAL_PREFIX) -> StoreKind.OFFICIAL
            id.startsWith(BRAND_PREFIX) -> StoreKind.BRAND
            id.startsWith(DIGITAL_PREFIX) -> StoreKind.DIGITAL
            else -> StoreKind.AMAZGONE
        }
        return Store(id, name, kind)
    }

    private fun digital(sellerId: String): Store? =
        DIGITAL_STORES[sellerId]?.let { Store(DIGITAL_PREFIX + sellerId, it, StoreKind.DIGITAL) }

    private fun slug(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
