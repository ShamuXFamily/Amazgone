package com.cikup.amazgone.account.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.account_achievements
import amazgone.shared.generated.resources.account_guest_body
import amazgone.shared.generated.resources.account_guest_carry_over
import amazgone.shared.generated.resources.account_hello_guest
import amazgone.shared.generated.resources.account_hello_user
import amazgone.shared.generated.resources.account_leaderboard
import amazgone.shared.generated.resources.account_orders
import amazgone.shared.generated.resources.account_session_expired
import amazgone.shared.generated.resources.account_sign_out
import amazgone.shared.generated.resources.account_sync_now
import amazgone.shared.generated.resources.account_wallet
import amazgone.shared.generated.resources.account_wishlist
import amazgone.shared.generated.resources.wallet_balance
import amazgone.shared.generated.resources.wallet_pending
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.syncStatusLabel
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.progress.presentation.LevelBadge
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.guestContent(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    item(key = "guest-header") {
        Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), modifier = Modifier.staggeredEnter(2)) {
            Text(stringResource(Res.string.account_hello_guest), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(Res.string.account_guest_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    state.expiredUsername?.let { name ->
        item(key = "expired") { Text(stringResource(Res.string.account_session_expired, name), color = MaterialTheme.colorScheme.error) }
    }
    item(key = "form") { AuthFormCard(state.form, state.isRemoteAvailable, onIntent, Modifier.staggeredEnter(3)) }
    item(key = "carry") {
        Text(stringResource(Res.string.account_guest_carry_over), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun LazyListScope.signedInHeader(state: AccountState) {
    val session = state.session ?: return
    item(key = "profile") { ProfileHeader(session.username, state.xp, Modifier.staggeredEnter(0)) }
}

fun LazyListScope.signedInFooter(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    item(key = "sync") {
        ElevatedCard {
            ListItem(
                headlineContent = { Text(syncStatusLabel(state.syncStatus)) },
                trailingContent = {
                    FilledTonalButton(onClick = { onIntent(AccountIntent.SyncNow) }) { Text(stringResource(Res.string.account_sync_now)) }
                },
            )
        }
    }
    item(key = "signout") {
        ElevatedCard {
            MenuRow(Icons.AutoMirrored.Outlined.Logout, Res.string.account_sign_out, showChevron = false) { onIntent(AccountIntent.SignOut) }
        }
    }
}

@Composable
private fun ProfileHeader(username: String, xp: Long, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg)) {
        Box(
            Modifier.size(AmazgoneDimens.iconXl * 2 / 3).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(username.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Text(stringResource(Res.string.account_hello_user, username), style = MaterialTheme.typography.titleLarge)
            LevelBadge(xp)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: StringResource, showChevron: Boolean = true, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (showChevron) {
            { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) }
        } else {
            null
        },
        modifier = Modifier.clickableRow(onClick),
    )
}
