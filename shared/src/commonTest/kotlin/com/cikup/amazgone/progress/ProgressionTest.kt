package com.cikup.amazgone.progress

import com.cikup.amazgone.progress.domain.model.LevelCurve
import com.cikup.amazgone.progress.domain.model.XpRules
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressionTest {
    @Test
    fun levelThresholdsFollowTheQuadraticCurve() {
        assertEquals(1, LevelCurve.levelFor(0))
        assertEquals(1, LevelCurve.levelFor(99))
        assertEquals(2, LevelCurve.levelFor(100))
        assertEquals(3, LevelCurve.levelFor(400))
        assertEquals(10, LevelCurve.levelFor(8_100))
        assertEquals(LevelCurve.MAX_LEVEL, LevelCurve.levelFor(Long.MAX_VALUE / 2))
    }

    @Test
    fun levelAndThresholdAreInverse() {
        (1..20).forEach { level -> assertEquals(level, LevelCurve.levelFor(LevelCurve.xpForLevel(level))) }
    }

    @Test
    fun progressIsAFractionOfTheCurrentLevel() {
        assertEquals(0f, LevelCurve.progress(100))
        assertEquals(0.5f, LevelCurve.progress(250))
    }

    @Test
    fun orderXpScalesWithSpendWithinBounds() {
        assertEquals(10, XpRules.forOrder(5))
        assertEquals(250, XpRules.forOrder(2_500))
        assertEquals(XpRules.MAX_XP_PER_WRITE, XpRules.forOrder(10_000_000))
    }
}
