package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.sync_error
import amazgone.shared.generated.resources.sync_offline
import amazgone.shared.generated.resources.sync_pending
import amazgone.shared.generated.resources.sync_synced
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.sync.domain.SyncStatus
import org.jetbrains.compose.resources.stringResource

/** Small cloud glyph for app bars; pulses while changes are waiting to sync. */
@Composable
fun SyncStatusIcon(status: SyncStatus, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = status,
        contentKey = { it::class },
        transitionSpec = { (fadeIn() + scaleIn(MotionTokens.bouncy())).togetherWith(fadeOut()) },
        modifier = modifier,
        label = "syncIcon",
    ) { current ->
        val pulse = if (current is SyncStatus.Pending && !LocalReduceMotion.current) {
            val alpha by rememberInfiniteTransition(label = "syncPulse").animateFloat(
                initialValue = 1f,
                targetValue = PULSE_MIN_ALPHA,
                animationSpec = infiniteRepeatable(tween(MotionTokens.DURATION_EXTRA_LONG_MS), RepeatMode.Reverse),
                label = "syncPulseAlpha",
            )
            Modifier.graphicsLayer { this.alpha = alpha }
        } else {
            Modifier
        }
        Icon(
            imageVector = when (current) {
                SyncStatus.Synced -> Icons.Outlined.CloudDone
                is SyncStatus.Pending -> Icons.Outlined.CloudSync
                is SyncStatus.Offline -> Icons.Outlined.CloudOff
                is SyncStatus.Error -> Icons.Outlined.ErrorOutline
            },
            contentDescription = syncStatusLabel(current),
            tint = if (current is SyncStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = pulse,
        )
    }
}

@Composable
fun syncStatusLabel(status: SyncStatus): String = when (status) {
    SyncStatus.Synced -> stringResource(Res.string.sync_synced)
    is SyncStatus.Pending -> stringResource(Res.string.sync_pending, status.count)
    is SyncStatus.Offline -> stringResource(Res.string.sync_offline, status.pendingCount)
    is SyncStatus.Error -> stringResource(Res.string.sync_error, status.pendingCount) // details are logged, not shown
}

private const val PULSE_MIN_ALPHA = 0.35f
