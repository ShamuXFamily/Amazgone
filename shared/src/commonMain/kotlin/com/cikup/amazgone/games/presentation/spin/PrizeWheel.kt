package com.cikup.amazgone.games.presentation.spin

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_wheel_a11y
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.games.domain.model.Reward
import com.cikup.amazgone.games.domain.model.WheelGeometry
import com.cikup.amazgone.games.presentation.rewardShortLabel
import org.jetbrains.compose.resources.stringResource

private const val LABEL_RADIUS = 0.68f
private const val HUB_RADIUS = 0.14f
private const val RIM_WIDTH = 0.035f
private const val POINTER_WIDTH = 0.09f
private const val POINTER_HEIGHT = 0.12f
private const val QUARTER_TURN = 90f

/** Wheel is rotated by [rotation]; the pointer (drawn upright) flicks by [pointerAngle]. */
@Composable
fun PrizeWheel(segments: List<Reward>, rotation: () -> Float, pointerAngle: () -> Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fills = listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer, scheme.surfaceContainerHighest)
    val onFills = listOf(scheme.onPrimaryContainer, scheme.onTertiaryContainer, scheme.onSecondaryContainer, scheme.onSurface)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val labels = segments.map(::rewardShortLabel)
    val description = stringResource(Res.string.game_wheel_a11y)
    Box(modifier.widthIn(max = AmazgoneDimens.contentMaxWidth / 2).fillMaxWidth().aspectRatio(1f).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val sweep = WheelGeometry.segmentSweep(segments.size)
            rotate(rotation()) {
                segments.indices.forEach { i ->
                    drawArc(fills[i % fills.size], startAngle = i * sweep - QUARTER_TURN, sweepAngle = sweep, useCenter = true)
                    drawLabel(measurer, labels[i], labelStyle.copy(color = onFills[i % onFills.size]), i * sweep + sweep / 2)
                }
            }
            drawCircle(scheme.outline, style = Stroke(size.minDimension * RIM_WIDTH))
            drawCircle(scheme.surface, radius = size.minDimension * HUB_RADIUS)
            drawCircle(scheme.tertiary, radius = size.minDimension * HUB_RADIUS, style = Stroke(size.minDimension * RIM_WIDTH))
            drawPointer(pointerAngle(), scheme.error)
        }
    }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, angle: Float) {
    val layout = measurer.measure(text, style)
    val radius = size.minDimension / 2 * LABEL_RADIUS
    rotate(angle, pivot = center) {
        val topLeft = Offset(center.x - layout.size.width / 2f, center.y - radius - layout.size.height / 2f)
        drawText(layout, topLeft = topLeft)
    }
}

private fun DrawScope.drawPointer(angle: Float, color: Color) {
    val w = size.minDimension * POINTER_WIDTH
    val h = size.minDimension * POINTER_HEIGHT
    val tip = Offset(center.x, h)
    rotate(angle, pivot = Offset(center.x, 0f)) {
        val path = Path().apply {
            moveTo(center.x - w / 2, 0f)
            lineTo(center.x + w / 2, 0f)
            lineTo(tip.x, tip.y)
            close()
        }
        drawPath(path, color)
    }
}

