package com.cikup.amazgone.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val CONFETTI_COUNT = 90
private const val GRAVITY = 1.6f
private const val SPREAD_RADIANS = PI.toFloat() * 0.9f
private const val MIN_SPEED = 0.55f
private const val MAX_SPEED = 1.25f
private const val PIECE_SIZE_PX = 18f
private const val SPIN_TURNS = 3f
private const val FULL_TURN = 360f

private data class Piece(val angle: Float, val speed: Float, val spin: Float, val colorIndex: Int, val wide: Boolean)

/**
 * Physics confetti burst from [origin] (fractions of the canvas). Restarts whenever [trigger] changes;
 * draws nothing with reduced motion. Colors come from the theme.
 */
@Composable
fun ConfettiBurst(trigger: Any, modifier: Modifier = Modifier, origin: Offset = Offset(0.5f, 0.35f)) {
    if (LocalReduceMotion.current) return
    val colors = with(MaterialTheme.colorScheme) { listOf(primary, tertiary, secondary, error, primaryContainer, tertiaryContainer) }
    val progress = remember(trigger) { Animatable(0f) }
    val pieces = remember(trigger) {
        val random = Random(trigger.hashCode())
        List(CONFETTI_COUNT) {
            Piece(
                angle = -PI.toFloat() / 2 + (random.nextFloat() - 0.5f) * SPREAD_RADIANS * 2,
                speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED),
                spin = (random.nextFloat() - 0.5f) * 2,
                colorIndex = random.nextInt(colors.size),
                wide = random.nextBoolean(),
            )
        }
    }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS * 2, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val start = Offset(size.width * origin.x, size.height * origin.y)
        val reach = size.minDimension
        pieces.forEach { piece ->
            val x = start.x + cos(piece.angle) * piece.speed * reach * t
            val y = start.y + sin(piece.angle) * piece.speed * reach * t + GRAVITY * reach * t * t
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(piece.spin * SPIN_TURNS * FULL_TURN * t, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[piece.colorIndex].copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = if (piece.wide) Size(PIECE_SIZE_PX, PIECE_SIZE_PX / 2) else Size(PIECE_SIZE_PX / 2, PIECE_SIZE_PX),
                )
            }
        }
    }
}

/** Circle then check stroke drawn progressively (0..1). */
@Composable
fun AnimatedCheckmark(color: Color, modifier: Modifier = Modifier, strokeWidthPx: Float = 12f) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) progress.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing))
    }
    Canvas(modifier) {
        val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        val circleShare = CIRCLE_SHARE
        val circleProgress = (progress.value / circleShare).coerceIn(0f, 1f)
        drawArc(color, -90f, FULL_TURN * circleProgress, useCenter = false, style = stroke)
        val checkProgress = ((progress.value - circleShare) / (1 - circleShare)).coerceIn(0f, 1f)
        if (checkProgress > 0f) {
            val check = Path().apply {
                moveTo(size.width * CHECK_START_X, size.height * CHECK_MID_Y)
                lineTo(size.width * CHECK_MID_X, size.height * CHECK_END_Y)
                lineTo(size.width * CHECK_END_X, size.height * CHECK_START_Y)
            }
            val measure = PathMeasure().apply { setPath(check, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * checkProgress, partial, true)
            drawPath(partial, color, style = stroke)
        }
    }
}

private const val CIRCLE_SHARE = 0.55f
private const val CHECK_START_X = 0.28f
private const val CHECK_MID_X = 0.44f
private const val CHECK_END_X = 0.74f
private const val CHECK_START_Y = 0.36f
private const val CHECK_MID_Y = 0.52f
private const val CHECK_END_Y = 0.68f
