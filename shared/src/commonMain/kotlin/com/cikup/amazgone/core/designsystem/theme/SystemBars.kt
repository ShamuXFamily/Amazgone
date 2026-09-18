package com.cikup.amazgone.core.designsystem.theme

import androidx.compose.runtime.Composable

/** Keeps status/navigation bar icons readable when the app theme differs from the device theme. */
@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean, followSystem: Boolean)
