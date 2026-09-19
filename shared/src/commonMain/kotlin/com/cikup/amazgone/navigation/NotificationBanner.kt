package com.cikup.amazgone.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.presentation.KindBadge
import kotlinx.coroutines.delay

private const val BANNER_VISIBLE_MS = 4_500L
private val BANNER_MAX_WIDTH = AmazgoneDimens.productCardMinWidth * 3

/**
 * In-app version of a push notification: slides down from the top, taps open its screen,
 * swiping up or waiting dismisses it. Keeps the last banner around so the exit animation has content.
 */
@Composable
fun NotificationBanner(banner: AppNotification?, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(banner) }
    if (banner != null) shown = banner
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(banner?.id) {
        if (banner == null) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(BANNER_VISIBLE_MS)
        onDismiss()
    }
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically(MotionTokens.bouncy()) { -it } + fadeIn(),
        exit = slideOutVertically(MotionTokens.snappy()) { -it } + fadeOut(),
        modifier = modifier.statusBarsPadding().padding(horizontal = AmazgoneDimens.spaceMd, vertical = AmazgoneDimens.spaceSm),
    ) {
        shown?.let { item ->
            Surface(
                onClick = onOpen,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = AmazgoneDimens.spaceSm,
                modifier = Modifier.widthIn(max = BANNER_MAX_WIDTH).fillMaxWidth().pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount -> if (dragAmount < 0) onDismiss() }
                },
            ) {
                Row(Modifier.padding(AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd), verticalAlignment = Alignment.CenterVertically) {
                    KindBadge(item.kind)
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
