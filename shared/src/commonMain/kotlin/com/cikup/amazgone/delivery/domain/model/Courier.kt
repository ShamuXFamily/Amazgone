package com.cikup.amazgone.delivery.domain.model

/**
 * Couriers through history, in chronological order. Speeds are rough real-world daily distances
 * (a pigeon flies ~60 km/h for ~10 h a day, a Pony Express relay covered ~400 km a day, a clipper
 * ~450 km a day…). Rocket and teleporter are future tech with a flat trip time.
 * Starter couriers are free; the rest are unlocked once in the Garage (prices mirror firestore.rules).
 */
enum class Courier(
    val emoji: String,
    /** Year of introduction (negative = BCE). */
    val year: Int,
    val kmPerDay: Double,
    val priceCoins: Long,
    /** Only short hops (null = unlimited). */
    val maxRangeKm: Double? = null,
    /** Fixed trip time regardless of distance. */
    val flatMillis: Long? = null,
    /** Packing time before it leaves (future couriers load instantly). */
    val handlingMillis: Long = CourierPlan.HANDLING_MILLIS,
) {
    CAMEL("🐫", year = -1000, kmPerDay = 40.0, priceCoins = 0),
    PIGEON("🕊️", year = -776, kmPerDay = 600.0, priceCoins = 0),
    RELAY_RUNNER("🏃", year = 1450, kmPerDay = 240.0, priceCoins = 100),
    STEAM_TRAIN("🚂", year = 1830, kmPerDay = 800.0, priceCoins = 600),
    CLIPPER_SHIP("⛵", year = 1845, kmPerDay = 450.0, priceCoins = 250),
    PONY_EXPRESS("🐎", year = 1860, kmPerDay = 400.0, priceCoins = 300),
    MAIL_TRUCK("🚚", year = 1906, kmPerDay = 900.0, priceCoins = 900),
    AIRMAIL_BIPLANE("🛩️", year = 1918, kmPerDay = 2_400.0, priceCoins = 1_500),
    CARGO_JET("✈️", year = 1958, kmPerDay = 15_000.0, priceCoins = 3_500),
    DRONE("🚁", year = 2013, kmPerDay = 1_440.0, priceCoins = 2_000, maxRangeKm = 50.0),
    ROCKET("🚀", year = 2035, kmPerDay = 0.0, priceCoins = 8_000, flatMillis = 60 * 60 * 1_000L, handlingMillis = 15 * 60 * 1_000L),
    TELEPORTER("✨", year = 2100, kmPerDay = 0.0, priceCoins = 20_000, flatMillis = 60 * 1_000L, handlingMillis = 0),
    ;

    val isStarter: Boolean get() = priceCoins == 0L

    companion object {
        val STARTERS: Set<Courier> = entries.filter { it.isStarter }.toSet()

        fun parse(value: String?): Courier? = entries.firstOrNull { it.name == value }
    }
}

/** Timing rules shared by checkout (estimates) and tracking (live position). */
object CourierPlan {
    /** Usual packing time before a courier leaves the warehouse. */
    const val HANDLING_MILLIS = 2 * 60 * 60 * 1_000L
    private const val DAY_MILLIS = 24 * 60 * 60 * 1_000.0
    private const val MIN_TRAVEL_MILLIS = 10 * 60 * 1_000L

    /** Travel time for [km], or null when the courier can't cover that distance (drone range). */
    fun travelMillis(courier: Courier, km: Double): Long? {
        courier.flatMillis?.let { return it }
        courier.maxRangeKm?.let { if (km > it) return null }
        return ((km / courier.kmPerDay) * DAY_MILLIS).toLong().coerceAtLeast(MIN_TRAVEL_MILLIS)
    }

    fun arrivalAt(placedAt: Long, courier: Courier, km: Double): Long? =
        travelMillis(courier, km)?.let { placedAt + courier.handlingMillis + it }

    /** 0 while packing (until [departsAt]), then linear to 1 on arrival. */
    fun progress(departsAt: Long, arrivalAt: Long, now: Long): Double = when {
        now >= arrivalAt -> 1.0
        now <= departsAt -> 0.0
        else -> (now - departsAt).toDouble() / (arrivalAt - departsAt)
    }
}
