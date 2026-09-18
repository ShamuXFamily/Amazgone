package com.cikup.amazgone.games.presentation.hub

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.account_achievements
import amazgone.shared.generated.resources.account_leaderboard
import amazgone.shared.generated.resources.game_deal_body
import amazgone.shared.generated.resources.game_deal_title
import amazgone.shared.generated.resources.game_next_in
import amazgone.shared.generated.resources.game_ready
import amazgone.shared.generated.resources.game_scratch_body
import amazgone.shared.generated.resources.game_scratch_title
import amazgone.shared.generated.resources.game_spin_body
import amazgone.shared.generated.resources.game_spin_title
import amazgone.shared.generated.resources.games_subtitle
import amazgone.shared.generated.resources.games_title
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.CoinAmount
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.games.presentation.formatCountdown
import com.cikup.amazgone.progress.presentation.LevelBadge
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val READY_PULSE = 1.04f

@Composable
fun GamesHubRoute(onNavigate: (GameDestination) -> Unit, viewModel: GamesHubViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { (it as GamesHubEffect.Navigate).let { nav -> onNavigate(nav.destination) } } }
    GamesHubScreen(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(state: GamesHubState, onIntent: (GamesHubIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.games_title)) },
                actions = { CoinAmount(state.coins, Modifier.padding(end = AmazgoneDimens.spaceLg), animateChanges = true) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                Column(Modifier.staggeredEnter(0), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                    Text(stringResource(Res.string.games_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LevelBadge(state.xp)
                }
            }
            item {
                GameCard(
                    Icons.Outlined.Casino, Res.string.game_spin_title, stringResource(Res.string.game_spin_body), state.spinCooldown,
                    MaterialTheme.colorScheme.primaryContainer, Modifier.staggeredEnter(1),
                ) { onIntent(GamesHubIntent.Open(GameDestination.SPIN)) }
            }
            item {
                GameCard(
                    Icons.Outlined.Style, Res.string.game_scratch_title, stringResource(Res.string.game_scratch_body), state.scratchCooldown,
                    MaterialTheme.colorScheme.tertiaryContainer, Modifier.staggeredEnter(2),
                ) { onIntent(GamesHubIntent.Open(GameDestination.SCRATCH)) }
            }
            item { DealPreview(state, Modifier.staggeredEnter(3)) { onIntent(GamesHubIntent.Open(GameDestination.DEAL)) } }
            item {
                Row(Modifier.staggeredEnter(4), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                    SmallTile(Icons.Outlined.Leaderboard, Res.string.account_leaderboard, Modifier.weight(1f)) {
                        onIntent(GamesHubIntent.Open(GameDestination.LEADERBOARD))
                    }
                    SmallTile(Icons.Outlined.EmojiEvents, Res.string.account_achievements, Modifier.weight(1f)) {
                        onIntent(GamesHubIntent.Open(GameDestination.ACHIEVEMENTS))
                    }
                }
            }
        }
    }
}

/** Available games gently breathe to invite a tap; cooling-down ones show a live countdown. */
@Composable
private fun GameCard(
    icon: ImageVector,
    title: StringResource,
    body: String,
    cooldown: Long,
    container: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ready = cooldown <= 0
    val pulse = if (ready && !LocalReduceMotion.current) {
        rememberInfiniteTransition(label = "ready").animateFloat(
            1f, READY_PULSE, infiniteRepeatable(tween(MotionTokens.DURATION_EXTRA_LONG_MS), RepeatMode.Reverse), label = "readyPulse",
        ).value
    } else {
        1f
    }
    ElevatedCard(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().pressScale(interaction).graphicsLayer { scaleX = pulse; scaleY = pulse },
    ) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = container, shape = MaterialTheme.shapes.large) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(AmazgoneDimens.spaceMd).size(AmazgoneDimens.iconLg))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (ready) stringResource(Res.string.game_ready) else stringResource(Res.string.game_next_in, formatCountdown(cooldown)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun DealPreview(state: GamesHubState, modifier: Modifier, onClick: () -> Unit) {
    val deal = state.deal ?: return
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(deal.product.thumbnailUrl, deal.product.id, Modifier.size(AmazgoneDimens.iconXl).clip(MaterialTheme.shapes.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                    Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(stringResource(Res.string.game_deal_title), style = MaterialTheme.typography.titleLarge)
                }
                Text(stringResource(Res.string.game_deal_body, deal.extraPercent.toInt()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(deal.product.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(formatCountdown(deal.remainingMillis(state.now)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SmallTile(icon: ImageVector, label: StringResource, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(AmazgoneDimens.iconLg))
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        }
    }
}
