package com.cikup.amazgone.wishlist.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.wishlist_add
import amazgone.shared.generated.resources.wishlist_remove
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import org.jetbrains.compose.resources.stringResource

private const val POP_SCALE = 1.4f
private const val CONTAINER_ALPHA = 0.9f

/** Heart that pops with a spring when it becomes filled. */
@Composable
fun HeartButton(saved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(saved) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (saved && !reduceMotion) {
            scale.snapTo(POP_SCALE)
            scale.animateTo(1f, MotionTokens.bouncy())
        }
    }
    FilledTonalIconButton(
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = CONTAINER_ALPHA),
        ),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
            onToggle()
        },
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        Icon(
            if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(if (saved) Res.string.wishlist_remove else Res.string.wishlist_add),
            tint = if (saved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
