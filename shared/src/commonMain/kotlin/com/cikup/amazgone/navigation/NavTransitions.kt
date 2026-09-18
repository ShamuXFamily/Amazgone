package com.cikup.amazgone.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import com.cikup.amazgone.core.designsystem.motion.MotionTokens

private const val FADE_THROUGH_INITIAL_SCALE = 0.92f

/** M3 "fade through": outgoing fades quickly, incoming fades + scales up after it. */
internal fun fadeThroughEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        fadeIn(tween(MotionTokens.DURATION_SHORT_MS))
    } else {
        fadeIn(
            tween(
                durationMillis = MotionTokens.DURATION_MEDIUM_MS,
                delayMillis = MotionTokens.DURATION_SHORT_MS / 2,
                easing = MotionTokens.EmphasizedDecelerate,
            ),
        ) + scaleIn(
            animationSpec = tween(MotionTokens.DURATION_MEDIUM_MS, easing = MotionTokens.EmphasizedDecelerate),
            initialScale = FADE_THROUGH_INITIAL_SCALE,
        )
    }

internal fun fadeThroughExit(): ExitTransition =
    fadeOut(tween(MotionTokens.DURATION_SHORT_MS, easing = MotionTokens.EmphasizedAccelerate))
