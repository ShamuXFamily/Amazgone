package com.cikup.amazgone.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.rememberReduceMotion

/**
 * Root M3 Expressive theme. Feature code must read colors, type and shapes only through
 * `MaterialTheme.*`, spacing through [AmazgoneDimens], and motion through `MotionTokens`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AmazgoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    followSystem: Boolean = true,
    animateChanges: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val target = if (darkTheme) ThemeColors(DarkColorScheme, DarkExtendedColors) else ThemeColors(LightColorScheme, LightExtendedColors)
    val colors = animateThemeColors(target, animate = animateChanges && !reduceMotion)
    SystemBarsAppearance(darkTheme, followSystem)
    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalExtendedColors provides colors.extended,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colors.scheme,
            motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            shapes = AmazgoneShapes,
            typography = AmazgoneTypography,
            content = content,
        )
    }
}

/** Access to brand extended colors: `AmazgoneTheme.extended.cta`. */
object AmazgoneTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
