package com.cikup.amazgone

import androidx.compose.runtime.Composable
import coil3.compose.setSingletonImageLoaderFactory
import com.cikup.amazgone.core.designsystem.image.buildImageLoader
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.navigation.AppScaffold

@Composable
fun App() {
    setSingletonImageLoaderFactory { context -> buildImageLoader(context) }
    AmazgoneTheme {
        AppScaffold()
    }
}
