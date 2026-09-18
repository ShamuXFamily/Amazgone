package com.cikup.amazgone.progress.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.leaderboard_empty_body
import amazgone.shared.generated.resources.leaderboard_empty_title
import amazgone.shared.generated.resources.leaderboard_offline
import amazgone.shared.generated.resources.leaderboard_title
import amazgone.shared.generated.resources.leaderboard_xp
import amazgone.shared.generated.resources.leaderboard_you
import amazgone.shared.generated.resources.level_short
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.core.sync.domain.SyncStatus
import com.cikup.amazgone.progress.domain.model.LeaderboardEntry
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val PODIUM_SIZE = 3
private const val YOU_PULSE_MIN = 0.55f

@Composable
fun LeaderboardRoute(onBack: () -> Unit, viewModel: LeaderboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.leaderboard_title), { viewModel.onIntent(LeaderboardIntent.Back) }) }) { padding ->
        PullToRefreshBox(state.isRefreshing, { viewModel.onIntent(LeaderboardIntent.Refresh) }, Modifier.fillMaxSize().padding(padding)) {
            val entries = state.leaderboard.entries
            if (entries.isEmpty()) {
                MessageState(Icons.Outlined.Leaderboard, stringResource(Res.string.leaderboard_empty_title), stringResource(Res.string.leaderboard_empty_body))
            } else {
                LazyColumn(contentPadding = PaddingValues(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                    if (state.syncStatus is SyncStatus.Offline) {
                        item { Text(stringResource(Res.string.leaderboard_offline), color = MaterialTheme.colorScheme.tertiary) }
                    }
                    item { Podium(entries.take(PODIUM_SIZE), state.leaderboard.myUid) }
                    itemsIndexed(entries.drop(PODIUM_SIZE), key = { _, e -> e.uid }) { index, entry ->
                        RankRow(entry, entry.uid == state.leaderboard.myUid, Modifier.animateItem().staggeredEnter(index))
                    }
                }
            }
        }
    }
}

/** Top three rise from the floor in 2-1-3 order with springy heights. */
@Composable
private fun Podium(top: List<LeaderboardEntry>, myUid: String?) {
    val order = listOfNotNull(top.getOrNull(1), top.getOrNull(0), top.getOrNull(2))
    Row(Modifier.fillMaxWidth().padding(vertical = AmazgoneDimens.spaceLg), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), verticalAlignment = Alignment.Bottom) {
        order.forEachIndexed { position, entry ->
            val height = when (entry.rank) {
                1 -> AmazgoneDimens.iconXl * 1.6f
                2 -> AmazgoneDimens.iconXl * 1.2f
                else -> AmazgoneDimens.iconXl
            }
            PodiumColumn(entry, height, entry.uid == myUid, delayIndex = position, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumColumn(entry: LeaderboardEntry, height: Dp, isMe: Boolean, delayIndex: Int, modifier: Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val rise = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(entry.uid) {
        if (reduceMotion) return@LaunchedEffect
        delay(MotionTokens.staggerDelayMillis(delayIndex * 2).toLong())
        rise.animateTo(1f, MotionTokens.bouncy())
    }
    val color = when (entry.rank) {
        1 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Text(if (isMe) stringResource(Res.string.leaderboard_you) else entry.username, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(stringResource(Res.string.leaderboard_xp, Formatters.coins(entry.xp)), style = MaterialTheme.typography.labelMedium)
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { scaleY = rise.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                .clip(MaterialTheme.shapes.medium)
                .background(color),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text("#${entry.rank}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(AmazgoneDimens.spaceSm))
        }
    }
}

@Composable
private fun RankRow(entry: LeaderboardEntry, isMe: Boolean, modifier: Modifier) {
    val pulse = if (isMe && !LocalReduceMotion.current) {
        rememberInfiniteTransition(label = "me").animateFloat(1f, YOU_PULSE_MIN, infiniteRepeatable(tween(MotionTokens.DURATION_EXTRA_LONG_MS), RepeatMode.Reverse), label = "mePulse").value
    } else {
        1f
    }
    ListItem(
        leadingContent = { Text("#${entry.rank}", style = MaterialTheme.typography.titleMedium) },
        headlineContent = { Text(if (isMe) "${entry.username} (${stringResource(Res.string.leaderboard_you)})" else entry.username) },
        supportingContent = { Text(stringResource(Res.string.level_short, entry.level)) },
        trailingContent = { Text(stringResource(Res.string.leaderboard_xp, Formatters.coins(entry.xp)), style = MaterialTheme.typography.titleSmall) },
        colors = ListItemDefaults.colors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulse) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.clip(MaterialTheme.shapes.medium),
    )
}
