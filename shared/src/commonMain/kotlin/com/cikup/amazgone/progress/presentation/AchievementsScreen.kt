package com.cikup.amazgone.progress.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.achievement_progress
import amazgone.shared.generated.resources.achievements_title
import amazgone.shared.generated.resources.achievements_unlocked
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.OnScrimColor
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.progress.domain.model.Achievement
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val HALF_TURN = 180f
private const val SHINE_ALPHA = 0.35f
private const val CAMERA = 12f

@Composable
fun AchievementsRoute(onBack: () -> Unit, viewModel: AchievementsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.achievements_title), { viewModel.onIntent(AchievementsIntent.Back) }) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
            contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(Res.string.achievements_unlocked, state.unlockedCount, state.achievements.size), style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(state.achievements, key = { _, a -> a.id.name }) { index, achievement -> Badge(achievement, index) }
        }
    }
}

/**
 * Unlocked badges flip over (3D rotationY) from their locked face, staggered, then a shine
 * sweeps across; locked ones show progress towards the goal.
 */
@Composable
private fun Badge(achievement: Achievement, index: Int) {
    val reduceMotion = LocalReduceMotion.current
    val flip = remember { Animatable(if (achievement.isUnlocked && !reduceMotion) 0f else 1f) }
    val shine = remember { Animatable(-1f) }
    LaunchedEffect(achievement.isUnlocked) {
        if (!achievement.isUnlocked || reduceMotion) return@LaunchedEffect
        delay(MotionTokens.staggerDelayMillis(index).toLong())
        flip.animateTo(1f, MotionTokens.gentle())
        shine.animateTo(2f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = LinearEasing))
    }
    val label = achievement.id.label()
    val showFront = flip.value >= HALF_FLIP
    val container = if (achievement.isUnlocked && showFront) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = (1 - flip.value) * HALF_TURN
                cameraDistance = CAMERA * density
            }
            .drawWithContent {
                drawContent()
                if (shine.value in -1f..2f && achievement.isUnlocked) {
                    val x = size.width * shine.value
                    drawRect(
                        Brush.linearGradient(
                            listOf(OnScrimColor.copy(alpha = 0f), OnScrimColor.copy(alpha = SHINE_ALPHA), OnScrimColor.copy(alpha = 0f)),
                            start = Offset(x - size.width / 3, 0f),
                            end = Offset(x, size.height),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg).graphicsLayer { rotationY = if (showFront) 0f else HALF_TURN },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
        ) {
            Icon(
                label.icon,
                contentDescription = null,
                tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AmazgoneDimens.iconLg),
            )
            Text(stringResource(label.title), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(stringResource(label.description), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            if (!achievement.isUnlocked) {
                LinearProgressIndicator(progress = { achievement.fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(Res.string.achievement_progress, Formatters.coins(achievement.progress), Formatters.coins(achievement.id.target)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private const val HALF_FLIP = 0.5f
