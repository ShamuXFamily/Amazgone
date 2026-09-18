package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.flash_sold
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

private const val TRACK_ALPHA = 0.55f

/** Orange pill whose fill springs from empty to [fraction] the first time it appears (and on changes). */
@Composable
fun SoldBadge(fraction: Float, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val fill = remember { Animatable(if (reduceMotion) fraction else 0f) }
    LaunchedEffect(fraction) { if (!reduceMotion) fill.animateTo(fraction, MotionTokens.gentle()) else fill.snapTo(fraction) }
    val cta = AmazgoneTheme.extended.cta
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(cta.copy(alpha = TRACK_ALPHA))
            .drawBehind { drawRect(cta, size = Size(size.width * fill.value, size.height)) },
    ) {
        Text(
            stringResource(Res.string.flash_sold, (fill.value * PERCENT).toInt()),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = AmazgoneTheme.extended.onCta,
            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2),
        )
    }
}

private const val PERCENT = 100
