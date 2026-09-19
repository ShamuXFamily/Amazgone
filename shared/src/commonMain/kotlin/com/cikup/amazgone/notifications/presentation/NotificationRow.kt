package com.cikup.amazgone.notifications.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.notifications_ago
import amazgone.shared.generated.resources.notifications_ago_now
import amazgone.shared.generated.resources.notifications_days
import amazgone.shared.generated.resources.notifications_hours
import amazgone.shared.generated.resources.notifications_minutes
import amazgone.shared.generated.resources.notifications_remove
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import org.jetbrains.compose.resources.stringResource

/** One inbox entry: kind badge, title/body, time, unread dot. Swipe left to remove. */
@Composable
fun NotificationRow(item: AppNotification, now: Long, onIntent: (NotificationsIntent) -> Unit, modifier: Modifier = Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        onDismiss = { onIntent(NotificationsIntent.Remove(item.id)) },
        backgroundContent = { RemoveBackground() },
        modifier = modifier.clip(MaterialTheme.shapes.large),
    ) {
        val container by animateColorAsState(
            if (item.read) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerHigh,
            MotionTokens.gentle(),
            label = "unread",
        )
        Surface(onClick = { onIntent(NotificationsIntent.Open(item.id)) }, color = container, shape = MaterialTheme.shapes.large) {
            Row(Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceMd), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                KindBadge(item.kind)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs / 2)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(agoLabel(item.deliverAt, now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!item.read) Box(Modifier.size(AmazgoneDimens.spaceSm).background(AmazgoneTheme.extended.cta, CircleShape))
                    }
                    Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RemoveBackground() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = AmazgoneDimens.spaceLg), contentAlignment = Alignment.CenterEnd) {
        Icon(Icons.Rounded.Delete, stringResource(Res.string.notifications_remove), tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun KindBadge(kind: NotificationKind, modifier: Modifier = Modifier) {
    val group = kind.group()
    val tints = AmazgoneTheme.extended.tileTints
    val icons = AmazgoneTheme.extended.tileIcons
    Surface(color = tints[group % tints.size], contentColor = icons[group % icons.size], shape = CircleShape, modifier = modifier) {
        Icon(kind.icon(), contentDescription = null, modifier = Modifier.padding(AmazgoneDimens.spaceSm).size(AmazgoneDimens.iconMd))
    }
}

@Composable
private fun agoLabel(deliverAt: Long, now: Long): String {
    val (unit, amount) = timeAgo(deliverAt, now)
    val short = when (unit) {
        AgoUnit.NOW -> return stringResource(Res.string.notifications_ago_now)
        AgoUnit.MINUTES -> stringResource(Res.string.notifications_minutes, amount)
        AgoUnit.HOURS -> stringResource(Res.string.notifications_hours, amount)
        AgoUnit.DAYS -> stringResource(Res.string.notifications_days, amount)
    }
    return stringResource(Res.string.notifications_ago, short)
}

/** Colour family: orders, delivery, games, progress. */
private fun NotificationKind.group(): Int = when (this) {
    NotificationKind.ORDER_CONFIRMED, NotificationKind.ORDER_REJECTED -> 0
    NotificationKind.COURIER_DEPARTED, NotificationKind.PARCEL_DELIVERED, NotificationKind.REVIEW_PROMPT -> 1
    NotificationKind.SPIN_READY, NotificationKind.SCRATCH_READY, NotificationKind.FLASH_SALE -> 2
    NotificationKind.LEVEL_UP, NotificationKind.ACHIEVEMENT, NotificationKind.COURIER_UNLOCKED -> 3
}

private fun NotificationKind.icon(): ImageVector = when (this) {
    NotificationKind.ORDER_CONFIRMED -> Icons.Rounded.CheckCircle
    NotificationKind.ORDER_REJECTED -> Icons.Rounded.Replay
    NotificationKind.COURIER_DEPARTED -> Icons.Rounded.LocalShipping
    NotificationKind.PARCEL_DELIVERED -> Icons.Rounded.Inventory2
    NotificationKind.REVIEW_PROMPT -> Icons.Rounded.RateReview
    NotificationKind.SPIN_READY -> Icons.Rounded.Casino
    NotificationKind.SCRATCH_READY -> Icons.Rounded.Style
    NotificationKind.FLASH_SALE -> Icons.Rounded.Bolt
    NotificationKind.LEVEL_UP -> Icons.Rounded.TrendingUp
    NotificationKind.ACHIEVEMENT -> Icons.Rounded.EmojiEvents
    NotificationKind.COURIER_UNLOCKED -> Icons.Rounded.RocketLaunch
}
