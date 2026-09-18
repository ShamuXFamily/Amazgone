package com.cikup.amazgone.catalog.presentation.home

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.home_deliver_to
import amazgone.shared.generated.resources.quick_all_categories
import amazgone.shared.generated.resources.quick_orders
import amazgone.shared.generated.resources.quick_scratch
import amazgone.shared.generated.resources.quick_spin
import amazgone.shared.generated.resources.quick_wallet
import androidx.compose.foundation.layout.Arrangement
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

/** Wordmark + delivery chip + sync state, then the search pill. */
@Composable
fun HomeHeader(syncStatus: SyncStatus, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            BrandWordmark()
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(stringResource(Res.string.home_deliver_to), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            SyncStatusIcon(syncStatus)
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
