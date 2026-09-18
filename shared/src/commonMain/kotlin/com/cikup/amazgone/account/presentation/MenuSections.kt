package com.cikup.amazgone.account.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.account_achievements
import amazgone.shared.generated.resources.account_leaderboard
import amazgone.shared.generated.resources.account_orders
import amazgone.shared.generated.resources.account_wallet
import amazgone.shared.generated.resources.account_wishlist
import amazgone.shared.generated.resources.menu_buy_again
import amazgone.shared.generated.resources.menu_games_tile
import amazgone.shared.generated.resources.menu_roulette_body
import amazgone.shared.generated.resources.menu_roulette_cta
import amazgone.shared.generated.resources.menu_roulette_kicker
import amazgone.shared.generated.resources.menu_roulette_title
import amazgone.shared.generated.resources.menu_shop_by_category
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.games.presentation.spin.MiniWheel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TILE_ASPECT = 2.2f

/** Two brand tiles: navy coin wallet (live balance) and the game zone. */
@Composable
fun MenuTiles(coins: Long, onIntent: (AccountIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = AmazgoneTheme.extended
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
        BrandTile(onClick = { onIntent(AccountIntent.Open(AccountDestination.WALLET)) }, container = colors.brandNavy, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.account_wallet), style = MaterialTheme.typography.labelLarge, color = colors.onBrandNavyVariant)
            CoinAmount(coins, style = MaterialTheme.typography.titleLarge.copy(color = colors.onBrandNavy, fontWeight = FontWeight.Bold), animateChanges = true)
        }
        BrandTile(onClick = { onIntent(AccountIntent.Open(AccountDestination.GAMES)) }, container = colors.cta, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.SportsEsports, contentDescription = null, tint = colors.onCta)
            Text(stringResource(Res.string.menu_games_tile), style = MaterialTheme.typography.titleMedium, color = colors.onCta, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BrandTile(onClick: () -> Unit, container: Color, modifier: Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(onClick = onClick, interactionSource = interaction, color = container, shape = MaterialTheme.shapes.large, modifier = modifier.aspectRatio(TILE_ASPECT).pressScale(interaction)) {
        Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs, Alignment.CenterVertically)) { content() }
    }
}

/** Pastel promo with an endlessly turning mini wheel. */
@Composable
fun RouletteBanner(onIntent: (AccountIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = AmazgoneTheme.extended
    Surface(
        onClick = { onIntent(AccountIntent.Open(AccountDestination.SPIN)) },
        color = colors.tileTints[2],
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Text(stringResource(Res.string.menu_roulette_kicker), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(Res.string.menu_roulette_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(Res.string.menu_roulette_body), style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { onIntent(AccountIntent.Open(AccountDestination.SPIN)) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandNavy, contentColor = colors.onBrandNavy),
                ) { Text(stringResource(Res.string.menu_roulette_cta)) }
            }
            MiniWheel(Modifier.size(AmazgoneDimens.iconXl))
        }
    }
}

private data class MenuEntry(val icon: ImageVector, val label: StringResource, val destination: AccountDestination)

private val MENU = listOf(
    MenuEntry(Icons.Outlined.Receipt, Res.string.account_orders, AccountDestination.ORDERS),
    MenuEntry(Icons.Outlined.Replay, Res.string.menu_buy_again, AccountDestination.ORDERS),
    MenuEntry(Icons.Outlined.FavoriteBorder, Res.string.account_wishlist, AccountDestination.WISHLIST),
    MenuEntry(Icons.Outlined.Category, Res.string.menu_shop_by_category, AccountDestination.CATEGORIES),
    MenuEntry(Icons.Outlined.EmojiEvents, Res.string.account_achievements, AccountDestination.ACHIEVEMENTS),
    MenuEntry(Icons.Outlined.Leaderboard, Res.string.account_leaderboard, AccountDestination.LEADERBOARD),
)

@Composable
fun MenuList(onIntent: (AccountIntent) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column {
            MENU.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(entry.label)) },
                    leadingContent = { Icon(entry.icon, contentDescription = null, tint = AmazgoneTheme.extended.cta) },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.clickableRow { onIntent(AccountIntent.Open(entry.destination)) }.staggeredEnter(index),
                )
            }
        }
    }
}
