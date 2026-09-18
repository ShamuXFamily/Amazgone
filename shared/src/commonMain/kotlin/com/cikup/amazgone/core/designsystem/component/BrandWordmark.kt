package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.app_name
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

private const val SMILE_START = 0.08f
private const val SMILE_END = 0.78f
private const val SMILE_DEPTH = 0.22f
private const val STROKE_FRACTION = 0.07f

/** Lower-case wordmark with an orange "smile" swoosh that draws itself on first appearance. */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) progress.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing)) }
    val smile = AmazgoneTheme.extended.cta
    Box(
        modifier.drawWithContent {
            drawContent()
            val y = size.height * (1 - SMILE_DEPTH / 2)
            val path = Path().apply {
                moveTo(size.width * SMILE_START, y - size.height * SMILE_DEPTH / 2)
                quadraticTo(size.width * (SMILE_START + SMILE_END) / 2, y + size.height * SMILE_DEPTH, size.width * SMILE_END, y - size.height * SMILE_DEPTH / 2)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * progress.value, partial, true)
            drawPath(partial, smile, style = Stroke(size.height * STROKE_FRACTION, cap = StrokeCap.Round))
            if (progress.value > 0.95f) {
                val tip = measure.getPosition(measure.length)
                drawCircle(smile, radius = size.height * STROKE_FRACTION, center = Offset(tip.x, tip.y))
            }
        },
    ) {
        Text(
            stringResource(Res.string.app_name).lowercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
    }
}
