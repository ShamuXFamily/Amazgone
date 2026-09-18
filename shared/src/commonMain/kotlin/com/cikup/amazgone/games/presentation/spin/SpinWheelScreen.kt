package com.cikup.amazgone.games.presentation.spin

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.game_cooldown
import amazgone.shared.generated.resources.game_spin_button
import amazgone.shared.generated.resources.game_spin_title
import amazgone.shared.generated.resources.game_spinning
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.motion.ConfettiBurst
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.games.domain.model.WheelGeometry
import com.cikup.amazgone.games.presentation.RewardReveal
import com.cikup.amazgone.games.presentation.formatCountdown
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val EXTRA_TURNS = 6
private const val SPIN_DURATION_MS = 4_200
private const val OVERSHOOT_DEGREES = 4f
private const val POINTER_KICK_DEGREES = -14f

@Composable
fun SpinWheelRoute(onBack: () -> Unit, viewModel: SpinViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onBack() } }
    SpinWheelScreen(state, viewModel::onIntent)
}

@Composable
fun SpinWheelScreen(state: SpinState, onIntent: (SpinIntent) -> Unit) {
    val rotation = remember { Animatable(0f) }
    val pointerKick = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current

    // Physics-ish spin: long deceleration past the target, then a springy settle back onto it.
    LaunchedEffect(state.spinId) {
        val index = state.targetIndex ?: return@LaunchedEffect
        if (state.spinId == 0) return@LaunchedEffect
        val target = WheelGeometry.targetRotation(rotation.value, index, state.segments.size, EXTRA_TURNS)
        if (reduceMotion) {
            rotation.snapTo(target)
        } else {
            rotation.animateTo(target + OVERSHOOT_DEGREES, tween(SPIN_DURATION_MS, easing = MotionTokens.EmphasizedDecelerate))
            rotation.animateTo(target, MotionTokens.bouncy())
        }
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onIntent(SpinIntent.Landed)
    }
    // Tick + pointer flick every time a segment boundary passes the pointer.
    LaunchedEffect(Unit) {
        snapshotFlow { WheelGeometry.segmentAt(rotation.value, state.segments.size) }
            .distinctUntilChanged()
            .collect {
                if (!rotation.isRunning) return@collect
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                launch {
                    pointerKick.snapTo(POINTER_KICK_DEGREES)
                    pointerKick.animateTo(0f, MotionTokens.bouncy())
                }
            }
    }

    Scaffold(topBar = { BackTopBar(stringResource(Res.string.game_spin_title), { onIntent(SpinIntent.Back) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().padding(AmazgoneDimens.spaceXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXl, Alignment.CenterVertically),
            ) {
                PrizeWheel(state.segments, rotation = { rotation.value }, pointerAngle = { pointerKick.value })
                AnimatedContent(targetState = state.wonReward, label = "spinResult") { reward ->
                    if (reward != null) RewardReveal(reward, state.wonXp)
                }
                Button(onClick = { onIntent(SpinIntent.Spin) }, enabled = state.canSpin, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            state.isSpinning -> stringResource(Res.string.game_spinning)
                            state.cooldown > 0 -> stringResource(Res.string.game_cooldown, formatCountdown(state.cooldown))
                            else -> stringResource(Res.string.game_spin_button)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            state.wonReward?.let { ConfettiBurst(trigger = state.spinId) }
        }
    }
}
