package com.cikup.amazgone.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow

/**
 * iOS derives status bar icon colour from the window's interface style, so a forced app theme
 * overrides the window style; following the system clears the override.
 */
@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean, followSystem: Boolean) {
    SideEffect {
        val style = when {
            followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            darkTheme -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
        UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().forEach { it.overrideUserInterfaceStyle = style }
    }
}
