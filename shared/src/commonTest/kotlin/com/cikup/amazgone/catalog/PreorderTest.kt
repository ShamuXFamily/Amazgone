package com.cikup.amazgone.catalog

import com.cikup.amazgone.catalog.domain.model.ProductDetails
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailState
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.testing.product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreorderTest {
    private val releaseDay = 1_792_713_600_000L // 2026-10-23 UTC
    private val duo = product("duo", price = 1999.0).copy(details = ProductDetails(releaseDateMillis = releaseDay))

    @Test
    fun preorderUntilReleaseDay() {
        assertTrue(duo.isPreorderAt(releaseDay - 1))
        assertFalse(duo.isPreorderAt(releaseDay))
        assertFalse(product("old", price = 1.0).isPreorderAt(0)) // no release date = normal stock
    }

    @Test
    fun detailStateNeedsAClockBeforeFlaggingPreorder() {
        val state = ProductDetailState("id", "grid", product = duo)
        assertFalse(state.isPreorder) // clock not read yet
        assertTrue(state.copy(now = releaseDay - 1).isPreorder)
    }

    @Test
    fun shortDateIsDayMonthYear() {
        assertEquals("23 Oct 2026", Formatters.shortDate(releaseDay, utc = true))
        assertEquals("Fri, 23 Oct", Formatters.weekdayDate(releaseDay, utc = true))
    }
}
