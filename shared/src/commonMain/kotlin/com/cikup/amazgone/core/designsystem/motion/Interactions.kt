package com.cikup.amazgone.core.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import kotlinx.coroutines.launch

private const val SHAKE_OFFSET_PX = 18f

/** Horizontal shake whenever [trigger] increments (e.g. invalid input). */
fun Modifier.shakeOnChange(trigger: Int): Modifier = composed {
    val offset = remember { Animatable(0f) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(trigger) {
        if (trigger == 0 || reduceMotion) return@LaunchedEffect
        offset.animateTo(
            0f,
            keyframes {
                durationMillis = MotionTokens.DURATION_LONG_MS
                -SHAKE_OFFSET_PX at 50
                SHAKE_OFFSET_PX at 150
                -SHAKE_OFFSET_PX / 2 at 250
                SHAKE_OFFSET_PX / 2 at 350
            },
        )
    }
    graphicsLayer { translationX = offset.value }
}

private const val HOLD_DURATION_MS = 1_200

/**
 * Long-press-to-confirm button: a fill sweeps across while held and [onConfirm] fires when it
 * completes; releasing early rewinds. Accessibility users get a plain click action.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val confirm by rememberUpdatedState(onConfirm)
    var done by remember { mutableStateOf(false) }
    val container = if (enabled) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AmazgoneDimens.minTouchTarget + AmazgoneDimens.spaceSm)
            .semantics {
                role = Role.Button
                onClick(label) { if (enabled) confirm(); enabled }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    done = false
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fill = scope.launch {
                        progress.animateTo(1f, tween((HOLD_DURATION_MS * (1 - progress.value)).toInt(), easing = LinearEasing))
                        done = true
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        confirm()
                    }
                    waitForUpOrCancellation()
                    if (!done) {
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, MotionTokens.snappy()) }
                    }
                }
            },
    ) {
        val fillColor = MaterialTheme.colorScheme.tertiary
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.drawBehind {
                drawRect(fillColor, size = Size(size.width * progress.value, size.height))
            },
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceLg),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
