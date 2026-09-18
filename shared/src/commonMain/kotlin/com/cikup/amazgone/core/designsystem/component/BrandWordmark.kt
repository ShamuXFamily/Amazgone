package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.app_name
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val SWOOSH_HEIGHT = 9.dp
private val STROKE = 2.6.dp
private const val START_X = 0.04f
private const val END_X = 0.86f
private const val SAG = 0.95f
private const val HEAD_LENGTH_FRACTION = 0.07f
private const val HEAD_ANGLE = 0.62f // radians (~35°)
private const val DRAW_SHARE = 0.85f

/**
 * Lower-case wordmark with an orange smile-arrow drawn in its own band under the letters
 * (never through descenders). The curve draws itself, then the arrowhead snaps in.
 */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) progress.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing))
    }
    var textWidth by remember { mutableIntStateOf(0) }
    val color = AmazgoneTheme.extended.cta
    val strokePx = with(LocalDensity.current) { STROKE.toPx() }
    Column(modifier) {
        Text(
            stringResource(Res.string.app_name).lowercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            onTextLayout = { textWidth = it.size.width },
        )
        Canvas(Modifier.width(with(LocalDensity.current) { textWidth.toDp() }).height(SWOOSH_HEIGHT)) {
            val start = Offset(size.width * START_X, strokePx)
            val end = Offset(size.width * END_X, strokePx)
            val control = Offset((start.x + end.x) / 2, size.height * 2 * SAG)
            val curve = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            }
            val measure = PathMeasure().apply { setPath(curve, false) }
            val drawn = (progress.value / DRAW_SHARE).coerceAtMost(1f)
            val partial = Path()
            measure.getSegment(0f, measure.length * drawn, partial, true)
            val stroke = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(partial, color, style = stroke)

            val headProgress = ((progress.value - DRAW_SHARE) / (1 - DRAW_SHARE)).coerceIn(0f, 1f)
            if (headProgress > 0f) {
                // arrowhead aligned with the curve's direction at the tip (control → end)
                val direction = atan2(end.y - control.y, end.x - control.x)
                val length = size.width * HEAD_LENGTH_FRACTION * headProgress
                val head = Path().apply {
                    moveTo(end.x - length * cos(direction - HEAD_ANGLE), end.y - length * sin(direction - HEAD_ANGLE))
                    lineTo(end.x, end.y)
                    lineTo(end.x - length * cos(direction + HEAD_ANGLE), end.y - length * sin(direction + HEAD_ANGLE))
                }
                drawPath(head, color, style = stroke)
            }
        }
    }
}
