package com.cikup.amazgone.games.presentation.scratch

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_card_a11y
import amazgone.shared.generated.resources.game_cooldown
import amazgone.shared.generated.resources.game_new_card
import amazgone.shared.generated.resources.game_scratch_hint
import amazgone.shared.generated.resources.game_scratch_title
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.games.domain.model.ScratchCoverage
import com.cikup.amazgone.games.domain.usecase.PlayOutcome
import com.cikup.amazgone.games.presentation.RewardReveal
import com.cikup.amazgone.games.presentation.formatCountdown
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val CARD_ASPECT = 1.6f
private const val BRUSH_FRACTION = 0.14f

@Composable
fun ScratchCardRoute(onBack: () -> Unit, viewModel: ScratchViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    Scaffold(topBar = { BackTopBar(stringResource(Res.string.game_scratch_title), { viewModel.onIntent(ScratchIntent.Back) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().padding(AmazgoneDimens.spaceXl),
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXl, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val card = state.card
                if (card != null) {
                    ScratchSurface(card, state.revealed, onRevealed = { viewModel.onIntent(ScratchIntent.Revealed) })
                }
                Button(
                    onClick = { viewModel.onIntent(ScratchIntent.NewCard) },
                    enabled = state.cooldown <= 0 && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.cooldown > 0) stringResource(Res.string.game_cooldown, formatCountdown(state.cooldown))
                        else stringResource(Res.string.game_new_card),
                    )
                }
            }
            if (state.revealed) state.card?.let { ConfettiBurst(trigger = it.play.id) }
        }
    }
}

/** Prize underneath, metallic foil on top erased by the finger; auto-completes past the threshold. */
@Composable
private fun ScratchSurface(card: PlayOutcome, revealed: Boolean, onRevealed: () -> Unit) {
    // each drag is one stroke; drawn as a continuous round-capped line so fast swipes leave no gaps
    val strokes = remember(card.play.id) { mutableStateListOf<List<Offset>>() }
    val coverage = remember(card.play.id) { ScratchCoverage() }
    val foilAlpha = remember(card.play.id) { Animatable(1f) }
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val hint = stringResource(Res.string.game_scratch_hint)
    val hintStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val foil = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.outline),
    )
    val description = stringResource(Res.string.game_card_a11y)

    LaunchedEffect(revealed) {
        if (revealed) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            foilAlpha.animateTo(0f, tween(MotionTokens.DURATION_LONG_MS))
        }
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(CARD_ASPECT).clip(MaterialTheme.shapes.extraLarge).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        RewardReveal(card.play.reward, card.play.xp, Modifier.padding(AmazgoneDimens.spaceLg))
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = foilAlpha.value }
                .pointerInput(card.play.id) {
                    fun cover(from: Offset, to: Offset) {
                        val steps = ((to - from).getDistance() / (minOf(size.width, size.height) * BRUSH_FRACTION / 2)).toInt().coerceAtLeast(1)
                        for (i in 0..steps) {
                            val p = from + (to - from) * (i.toFloat() / steps)
                            coverage.scratch(p.x / size.width, p.y / size.height)
                        }
                        if (!revealed && coverage.fraction >= ScratchCoverage.REVEAL_THRESHOLD) onRevealed()
                    }
                    detectDragGestures(
                        onDragStart = { start ->
                            strokes += listOf(start)
                            cover(start, start)
                        },
                    ) { change, _ ->
                        val current = strokes.lastOrNull() ?: return@detectDragGestures
                        val previous = current.last()
                        strokes[strokes.lastIndex] = current + change.position
                        cover(previous, change.position)
                    }
                }
                .pointerInput(card.play.id) { detectTapGestures { if (!revealed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } },
        ) {
            drawRect(foil)
            val layout = measurer.measure(hint, hintStyle)
            drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
            val radius = size.minDimension * BRUSH_FRACTION
            // BlendMode.Clear ignores the color; any opaque one works
            strokes.forEach { stroke ->
                drawCircle(color = hintStyle.color, radius = radius, center = stroke.first(), blendMode = BlendMode.Clear)
                stroke.zipWithNext { a, b ->
                    drawLine(hintStyle.color, a, b, strokeWidth = radius * 2, cap = StrokeCap.Round, blendMode = BlendMode.Clear)
                }
            }
        }
    }
}
