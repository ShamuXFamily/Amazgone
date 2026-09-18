package com.cikup.amazgone.games.presentation.spin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme

private const val SEGMENTS = 8
private const val FULL_TURN = 360f
private const val TURN_MS = 9_000
private const val HUB = 0.18f
private const val RIM = 0.06f

/** Decorative wheel for promo banners: slowly, endlessly rotating (static with reduce-motion). */
@Composable
fun MiniWheel(modifier: Modifier = Modifier) {
    val rotation = if (LocalReduceMotion.current) {
        0f
    } else {
        rememberInfiniteTransition(label = "miniWheel").animateFloat(
            0f, FULL_TURN, infiniteRepeatable(tween(TURN_MS, easing = LinearEasing), RepeatMode.Restart), label = "miniWheelTurn",
        ).value
    }
    val scheme = MaterialTheme.colorScheme
    val brand = AmazgoneTheme.extended
    val fills = listOf(brand.brandNavy, brand.cta, scheme.tertiary, scheme.surfaceContainerLowest)
    Canvas(modifier) {
        val sweep = FULL_TURN / SEGMENTS
        rotate(rotation) {
            repeat(SEGMENTS) { i -> drawArc(fills[i % fills.size], i * sweep, sweep, useCenter = true) }
        }
        drawCircle(scheme.surfaceContainerLowest, style = Stroke(size.minDimension * RIM))
        drawCircle(scheme.surfaceContainerLowest, radius = size.minDimension * HUB)
    }
}
