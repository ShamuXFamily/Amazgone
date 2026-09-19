package com.cikup.amazgone.catalog.presentation.home

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.home_deliver_to
import amazgone.shared.generated.resources.quick_all_categories
import amazgone.shared.generated.resources.quick_orders
import amazgone.shared.generated.resources.quick_scratch
import amazgone.shared.generated.resources.quick_spin
import amazgone.shared.generated.resources.quick_wallet
import amazgone.shared.generated.resources.notifications_bell
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.cikup.amazgone.core.designsystem.component.BrandWordmark
import com.cikup.amazgone.core.designsystem.component.QuickActionTile
import com.cikup.amazgone.core.designsystem.component.SyncStatusIcon
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.sync.domain.SyncStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Bell with an unread count that pops in; the icon wiggles when the count goes up. */
@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit) {
    val wiggle = remember { Animatable(0f) }
    var last by remember { mutableIntStateOf(unread) }
    LaunchedEffect(unread) {
        if (unread > last) {
            WIGGLE_DEGREES.forEach { wiggle.animateTo(it, MotionTokens.snappy()) }
        }
        last = unread
    }
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            AnimatedVisibility(unread > 0, enter = scaleIn(MotionTokens.bouncy()), exit = scaleOut()) {
                Badge(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta) {
                    Text(if (unread > MAX_BADGE) "$MAX_BADGE+" else unread.toString())
                }
            }
        }) {
            Icon(
                if (unread > 0) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
                stringResource(Res.string.notifications_bell),
                modifier = Modifier.graphicsLayer { rotationZ = wiggle.value },
            )
        }
    }
}

private const val MAX_BADGE = 9
private val WIGGLE_DEGREES = listOf(-14f, 12f, -8f, 5f, 0f)

/** Wordmark + delivery chip + sync state, then the search pill. */
@Composable
fun HomeHeader(syncStatus: SyncStatus, unread: Int, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            BrandWordmark()
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.home_deliver_to), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            SyncStatusIcon(syncStatus)
            NotificationBell(unread) { onIntent(HomeIntent.Open(HomeDestination.NOTIFICATIONS)) }
        }
        SearchPill(onClick = { onIntent(HomeIntent.Open(HomeDestination.SEARCH)) })
    }
}

private data class QuickAction(val icon: ImageVector, val label: StringResource, val destination: HomeDestination)

private val QUICK_ACTIONS = listOf(
    QuickAction(Icons.Outlined.Apps, Res.string.quick_all_categories, HomeDestination.CATEGORIES),
    QuickAction(Icons.Outlined.AccountBalanceWallet, Res.string.quick_wallet, HomeDestination.WALLET),
    QuickAction(Icons.Outlined.Casino, Res.string.quick_spin, HomeDestination.SPIN),
    QuickAction(Icons.Outlined.Style, Res.string.quick_scratch, HomeDestination.SCRATCH),
    QuickAction(Icons.Outlined.Receipt, Res.string.quick_orders, HomeDestination.ORDERS),
)

/** All shortcuts in one white card, evenly spaced (no scrolling), tiles cascade in. */
@Composable
fun QuickActions(onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val tints = AmazgoneTheme.extended.tileTints
    val iconColors = AmazgoneTheme.extended.tileIcons
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        shadowElevation = AmazgoneDimens.spaceXs / 4,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceXs, vertical = AmazgoneDimens.spaceXs)) {
            QUICK_ACTIONS.forEachIndexed { index, action ->
                QuickActionTile(
                    icon = action.icon,
                    label = stringResource(action.label),
                    tint = tints[index % tints.size],
                    iconColor = iconColors[index % iconColors.size],
                    index = index,
                    onClick = { onIntent(HomeIntent.Open(action.destination)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
