package com.cikup.amazgone.core.presentation

import com.cikup.amazgone.core.presentation.format.Formatters
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {
    @Test
    fun coinsAreGroupedByThousands() {
        assertEquals("0", Formatters.coins(0))
        assertEquals("999", Formatters.coins(999))
        assertEquals("1,000", Formatters.coins(1_000))
        assertEquals("1,234,567", Formatters.coins(1_234_567))
        assertEquals("-5,000", Formatters.coins(-5_000))
    }

    @Test
    fun compactCountsUseOneDecimal() {
        assertEquals("950", Formatters.compactCount(950))
        assertEquals("1K", Formatters.compactCount(1_000))
        assertEquals("12.3K", Formatters.compactCount(12_345))
    }

    @Test
    fun ratingsRoundToOneDecimal() {
        assertEquals("4.6", Formatters.rating(4.56))
        assertEquals("5.0", Formatters.rating(5.0))
    }

    @Test
    fun categorySlugsBecomeTitles() {
        assertEquals("Home Decoration", Formatters.categoryLabel("home-decoration"))
        assertEquals("Video Games", Formatters.categoryLabel("video-games"))
        assertEquals("Mens Shirts", Formatters.categoryLabel("mens_shirts"))
    }
}
