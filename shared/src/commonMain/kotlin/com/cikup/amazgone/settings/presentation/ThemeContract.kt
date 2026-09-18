package com.cikup.amazgone.settings.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.settings.domain.model.ThemeMode

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** False until the stored choice is read, so the first real value applies without a crossfade. */
    val isLoaded: Boolean = false,
) : UiState

sealed interface ThemeIntent : UiIntent {
    data class Select(val mode: ThemeMode) : ThemeIntent
}

sealed interface ThemeEffect : UiEffect
