package com.cikup.amazgone.navigation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.level_up_body
import amazgone.shared.generated.resources.level_up_title
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.OnScrimColor
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val SCRIM_ALPHA = 0.7f
private const val AUTO_DISMISS_MS = 2_800L
private const val MEDAL_SPINS = 720f

/** Full-screen celebration: medal spins in on a spring, confetti, auto-dismisses (tap to skip). */
@Composable
fun LevelUpOverlay(level: Int, onDismiss: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val appear = remember(level) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(level) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (!reduceMotion) appear.animateTo(1f, MotionTokens.bouncy())
        delay(AUTO_DISMISS_MS)
        onDismiss()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA * appear.value.coerceIn(0f, 1f)))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            modifier = Modifier.padding(AmazgoneDimens.spaceXl).graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
            },
        ) {
            Icon(
                Icons.Rounded.MilitaryTech,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(AmazgoneDimens.iconXl * 1.5f).graphicsLayer { rotationY = (1 - appear.value) * MEDAL_SPINS },
            )
            Text(stringResource(Res.string.level_up_title), style = MaterialTheme.typography.displaySmall, color = OnScrimColor)
            Text(stringResource(Res.string.level_up_body, level), style = MaterialTheme.typography.titleLarge, color = OnScrimColor)
        }
        ConfettiBurst(trigger = level)
    }
}
