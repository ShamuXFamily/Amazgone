package com.cikup.amazgone.settings.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.settings.domain.usecase.ObserveThemeModeUseCase
import com.cikup.amazgone.settings.domain.usecase.SetThemeModeUseCase

class ThemeViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : MviViewModel<ThemeState, ThemeIntent, ThemeEffect>(ThemeState()) {

    init {
        observeThemeMode().observe { setState { copy(mode = it, isLoaded = true) } }
    }

    override fun handleIntent(intent: ThemeIntent) {
        when (intent) {
            is ThemeIntent.Select -> {
                setState { copy(mode = intent.mode) } // optimistic: the switch reacts instantly
                launchSafely { setThemeMode(intent.mode) }
            }
        }
    }
}
