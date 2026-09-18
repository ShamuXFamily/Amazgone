package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.rating_a11y
import amazgone.shared.generated.resources.rating_count
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource

private const val STAR_COUNT = 5

/**
 * Five stars filled to [rating]; with [animate] they fill left-to-right on first appearance.
 */
@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
    count: Int? = null,
    starSize: Dp = AmazgoneDimens.iconSm,
    animate: Boolean = false,
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember(rating) { Animatable(if (animate && !reduceMotion) 0f else rating.toFloat()) }
    LaunchedEffect(rating) {
        if (animate && !reduceMotion) {
            progress.animateTo(rating.toFloat(), tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing))
        }
    }
    val description = stringResource(Res.string.rating_a11y, Formatters.rating(rating))
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        Row {
            repeat(STAR_COUNT) { index ->
                val fill = (progress.value - index).coerceIn(0f, 1f)
                Star(fill, starSize)
            }
        }
        if (count != null) {
            Text(
                text = stringResource(Res.string.rating_count, Formatters.compactCount(count.toLong())),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Star(fill: Float, size: Dp) {
    val filled = MaterialTheme.colorScheme.tertiary
    val empty = MaterialTheme.colorScheme.outlineVariant
    Box(Modifier.size(size)) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = empty, modifier = Modifier.size(size))
        Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            tint = filled,
            modifier = Modifier
                .size(size)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        color = filled,
                        topLeft = Offset(this.size.width * fill, 0f),
                        size = Size(this.size.width * (1 - fill), this.size.height),
                        blendMode = BlendMode.Clear,
                    )
                },
        )
    }
}
