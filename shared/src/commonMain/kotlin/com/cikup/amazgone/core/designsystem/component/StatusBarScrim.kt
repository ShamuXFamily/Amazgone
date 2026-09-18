package com.cikup.amazgone.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Solid band behind the status bar so edge-to-edge lists don't scroll under the clock. */
@Composable
fun StatusBarScrim(color: Color, modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(color))
}
