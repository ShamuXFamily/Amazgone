package com.cikup.amazgone.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** True when the OS asks for reduced motion; animations must then collapse to simple fades. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads the platform accessibility setting (Android animator scale, iOS Reduce Motion). */
@Composable
expect fun rememberReduceMotion(): Boolean
