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
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
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
