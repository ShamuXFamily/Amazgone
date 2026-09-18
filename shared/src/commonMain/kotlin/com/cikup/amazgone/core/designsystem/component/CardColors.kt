package com.cikup.amazgone.core.designsystem.component

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Card fill for every `ElevatedCard`. M3's default is the page colour, which only reads as a card
 * in light mode (thanks to the shadow); in dark mode the card would vanish into the background.
 */
@Composable
fun appCardColors(): CardColors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
