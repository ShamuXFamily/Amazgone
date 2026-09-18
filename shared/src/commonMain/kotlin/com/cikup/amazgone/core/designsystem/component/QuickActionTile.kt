package com.cikup.amazgone.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import kotlinx.coroutines.delay

private const val POP_FROM = 0.4f

/**
 * One shortcut: rounded-square pastel icon + one-line caption. Pops in (staggered by [index])
 * and springs down while pressed. Fills its slot so a row of them spaces evenly.
 */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    iconColor: Color,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduceMotion) 1f else POP_FROM) }
    val alpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        delay(MotionTokens.staggerDelayMillis(index).toLong())
        alpha.snapTo(1f)
        pop.animateTo(1f, MotionTokens.bouncy())
    }
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(vertical = AmazgoneDimens.spaceSm)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; this.alpha = alpha.value },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        Surface(color = tint, shape = MaterialTheme.shapes.medium, modifier = Modifier.pressScale(interaction)) {
            Box(Modifier.size(AmazgoneDimens.minTouchTarget), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(AmazgoneDimens.iconMd))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
