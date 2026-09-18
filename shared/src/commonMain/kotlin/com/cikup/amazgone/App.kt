package com.cikup.amazgone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.setSingletonImageLoaderFactory
import com.cikup.amazgone.core.designsystem.image.buildImageLoader
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.navigation.AppScaffold
import com.cikup.amazgone.settings.domain.model.ThemeMode
import com.cikup.amazgone.settings.presentation.ThemeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(themeViewModel: ThemeViewModel = koinViewModel()) {
    setSingletonImageLoaderFactory { context -> buildImageLoader(context) }
    val theme by themeViewModel.state.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    // Only crossfade user-driven changes, not the first read of the stored choice at launch.
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(theme.isLoaded) {
        if (theme.isLoaded) {
            withFrameNanos { }
            animate = true
        }
    }
    AmazgoneTheme(
        darkTheme = when (theme.mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        followSystem = theme.mode == ThemeMode.SYSTEM,
        animateChanges = animate,
    ) {
        AppScaffold()
    }
}
