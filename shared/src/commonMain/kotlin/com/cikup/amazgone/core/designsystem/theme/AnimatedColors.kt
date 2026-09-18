package com.cikup.amazgone.core.designsystem.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import com.cikup.amazgone.core.designsystem.motion.MotionTokens

/** Light/dark palettes crossfade instead of snapping. One progress value drives every colour. */
internal data class ThemeColors(val scheme: ColorScheme, val extended: ExtendedColors)

@Composable
internal fun animateThemeColors(target: ThemeColors, animate: Boolean): ThemeColors {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        // Start from whatever is on screen now, so a quick double toggle never jumps.
        from = blend(from, to, progress.value)
        to = target
        if (!animate) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(MotionTokens.DURATION_LONG_MS, easing = MotionTokens.StandardEasing))
        }
    }
    return if (progress.value >= 1f) to else blend(from, to, progress.value)
}

private fun blend(a: ThemeColors, b: ThemeColors, t: Float) = ThemeColors(blend(a.scheme, b.scheme, t), blend(a.extended, b.extended, t))

private fun blend(a: ExtendedColors, b: ExtendedColors, t: Float) = b.copy(
    brandNavy = lerp(a.brandNavy, b.brandNavy, t),
    onBrandNavy = lerp(a.onBrandNavy, b.onBrandNavy, t),
    onBrandNavyVariant = lerp(a.onBrandNavyVariant, b.onBrandNavyVariant, t),
    cta = lerp(a.cta, b.cta, t),
    onCta = lerp(a.onCta, b.onCta, t),
    success = lerp(a.success, b.success, t),
    imageStage = lerp(a.imageStage, b.imageStage, t),
    tileTints = a.tileTints.zip(b.tileTints) { x, y -> lerp(x, y, t) },
)

@Suppress("LongMethod") // one line per ColorScheme role
private fun blend(a: ColorScheme, b: ColorScheme, t: Float) = b.copy(
    primary = lerp(a.primary, b.primary, t),
    onPrimary = lerp(a.onPrimary, b.onPrimary, t),
    primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
    onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, t),
    inversePrimary = lerp(a.inversePrimary, b.inversePrimary, t),
    secondary = lerp(a.secondary, b.secondary, t),
    onSecondary = lerp(a.onSecondary, b.onSecondary, t),
    secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, t),
    onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, t),
    tertiary = lerp(a.tertiary, b.tertiary, t),
    onTertiary = lerp(a.onTertiary, b.onTertiary, t),
    tertiaryContainer = lerp(a.tertiaryContainer, b.tertiaryContainer, t),
    onTertiaryContainer = lerp(a.onTertiaryContainer, b.onTertiaryContainer, t),
    background = lerp(a.background, b.background, t),
    onBackground = lerp(a.onBackground, b.onBackground, t),
    surface = lerp(a.surface, b.surface, t),
    onSurface = lerp(a.onSurface, b.onSurface, t),
    surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, t),
    onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
    surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
    inverseSurface = lerp(a.inverseSurface, b.inverseSurface, t),
    inverseOnSurface = lerp(a.inverseOnSurface, b.inverseOnSurface, t),
    error = lerp(a.error, b.error, t),
    onError = lerp(a.onError, b.onError, t),
    errorContainer = lerp(a.errorContainer, b.errorContainer, t),
    onErrorContainer = lerp(a.onErrorContainer, b.onErrorContainer, t),
    outline = lerp(a.outline, b.outline, t),
    outlineVariant = lerp(a.outlineVariant, b.outlineVariant, t),
    scrim = lerp(a.scrim, b.scrim, t),
    surfaceBright = lerp(a.surfaceBright, b.surfaceBright, t),
    surfaceDim = lerp(a.surfaceDim, b.surfaceDim, t),
    surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, t),
    surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
    surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
    surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, t),
    surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
)
