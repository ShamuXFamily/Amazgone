package com.cikup.amazgone.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Single source of motion timing. Never hardcode durations, easings or spring values elsewhere.
 * Spatial movement should prefer `MaterialTheme.motionScheme`; these tokens cover what it does not
 * (list staggering, game physics, count-up tweens, celebrations).
 */
object MotionTokens {
    const val DURATION_SHORT_MS = 150
    const val DURATION_MEDIUM_MS = 300
    const val DURATION_LONG_MS = 500
    const val DURATION_EXTRA_LONG_MS = 900
    const val SHIMMER_CYCLE_MS = 1_200

    const val STAGGER_STEP_MS = 40
    /** Items past this index enter together so long lists do not feel sluggish. */
    const val STAGGER_MAX_INDEX = 8

    const val PRESS_SCALE = 0.96f
    const val ENTER_OFFSET_FRACTION = 0.15f

    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> bouncy(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> snappy(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    fun <T> gentle(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)

    /** Delay before list item [index] animates in; capped so late items do not wait forever. */
    fun staggerDelayMillis(index: Int): Int =
        index.coerceIn(0, STAGGER_MAX_INDEX) * STAGGER_STEP_MS
}
