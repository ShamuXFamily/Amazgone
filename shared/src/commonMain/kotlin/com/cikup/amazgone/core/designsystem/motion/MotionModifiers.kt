package com.cikup.amazgone.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SHIMMER_TRAVEL_PX = 1_000f

/** Loading skeleton sweep. Falls back to a static placeholder when reduce-motion is on. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    if (LocalReduceMotion.current) return@composed background(base)
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -SHIMMER_TRAVEL_PX,
        targetValue = SHIMMER_TRAVEL_PX * 2,
        animationSpec = infiniteRepeatable(
            tween(MotionTokens.SHIMMER_CYCLE_MS, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(progress, 0f),
            end = Offset(progress + SHIMMER_TRAVEL_PX, SHIMMER_TRAVEL_PX),
        ),
    )
}

/** Springy shrink while pressed; pair with the same [interactionSource] as the clickable. */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !LocalReduceMotion.current) MotionTokens.PRESS_SCALE else 1f,
        animationSpec = MotionTokens.bouncy(),
        label = "pressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Fade + rise on first composition, delayed by list position for a cascading entrance. */
fun Modifier.staggeredEnter(index: Int): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val rise = remember { Animatable(if (reduceMotion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(MotionTokens.staggerDelayMillis(index).toLong())
        launch { alpha.animateTo(1f, tween(MotionTokens.DURATION_MEDIUM_MS)) }
        rise.animateTo(0f, MotionTokens.gentle())
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = rise.value * size.height * MotionTokens.ENTER_OFFSET_FRACTION
    }
}

/** Moves content at a fraction of the scroll speed for a depth effect on hero images. */
fun Modifier.parallax(scrollOffsetPx: () -> Int, factor: Float = PARALLAX_FACTOR): Modifier =
    graphicsLayer { translationY = scrollOffsetPx() * factor }

private const val PARALLAX_FACTOR = 0.5f

