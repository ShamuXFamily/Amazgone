package com.cikup.amazgone.core.analytics

import com.cikup.amazgone.testing.product
import com.cikup.amazgone.core.common.AppLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingSink : AnalyticsSink {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    var lastUserId: String? = null
    override fun logEvent(name: String, params: Map<String, Any>) { events += name to params }
    override fun setUserId(id: String?) { lastUserId = id }
    override fun setUserProperty(name: String, value: String?) = Unit
}

private object SilentLogger : AppLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}

class AnalyticsTest {
    private val sink = RecordingSink()
    private val analytics = Analytics(sink, SilentLogger)

    @Test
    fun productEventsCarryGa4ItemParamsAndCoinsWithoutACurrency() {
        analytics.addToCart(product("1", price = 100.0).copy(brand = "Apple"), quantity = 2)
        val (name, params) = sink.events.single()
        assertEquals("add_to_cart", name)
        assertEquals("dummyjson:1", params["item_id"])
        assertEquals("Apple", params["item_brand"])
        assertEquals("official-apple", params["store_id"])
        assertEquals(2L, params["quantity"])
        assertFalse("currency" in params) // virtual coins must never show up as real revenue
    }

    @Test
    fun purchaseAlsoRecordsSpentCoins() {
        analytics.purchase("o1", totalCoins = 251, itemCount = 2, shippingTier = "EXPRESS", coupon = null, shipments = 2)
        assertEquals(listOf("purchase", "spend_virtual_currency"), sink.events.map { it.first })
        assertEquals(251L, sink.events.first().second["value"])
        assertFalse("coupon" in sink.events.first().second)
    }

    @Test
    fun blankSearchesAndZeroRewardsAreNotLogged() {
        analytics.search("  ")
        analytics.earnCoins(0, "spin")
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun aFailingSinkNeverCrashesTheApp() {
        val broken = Analytics(object : AnalyticsSink by NoopAnalyticsSink {
            override fun logEvent(name: String, params: Map<String, Any>) = error("SDK not ready")
        }, SilentLogger)
        broken.search("phone") // must not throw
    }

    @Test
    fun screenNamesComeFromTheRouteClass() {
        assertEquals("ProductDetail", com.cikup.amazgone.navigation.screenName("com.cikup.amazgone.navigation.Route.ProductDetail/{productId}/{origin}"))
        assertEquals("Search", com.cikup.amazgone.navigation.screenName("com.cikup.amazgone.navigation.Route.Search?category={category}"))
        assertEquals("Home", com.cikup.amazgone.navigation.screenName("com.cikup.amazgone.navigation.Route.Home"))
    }
}
