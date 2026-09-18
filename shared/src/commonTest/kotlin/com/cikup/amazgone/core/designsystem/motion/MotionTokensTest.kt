package com.cikup.amazgone.core.designsystem.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionTokensTest {

    @Test
    fun firstItemEntersImmediately() {
        assertEquals(0, MotionTokens.staggerDelayMillis(0))
    }

    @Test
    fun delayGrowsByStepPerIndex() {
        assertEquals(3 * MotionTokens.STAGGER_STEP_MS, MotionTokens.staggerDelayMillis(3))
    }

    @Test
    fun delayIsCappedForLongLists() {
        val cap = MotionTokens.STAGGER_MAX_INDEX * MotionTokens.STAGGER_STEP_MS
        assertEquals(cap, MotionTokens.staggerDelayMillis(500))
    }

    @Test
    fun negativeIndexIsTreatedAsFirst() {
        assertEquals(0, MotionTokens.staggerDelayMillis(-4))
    }
}
