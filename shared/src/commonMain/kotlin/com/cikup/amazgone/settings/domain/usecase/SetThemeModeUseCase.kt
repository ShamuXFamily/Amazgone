package com.cikup.amazgone.settings.domain.usecase

import com.cikup.amazgone.settings.domain.model.ThemeMode
import com.cikup.amazgone.settings.domain.repository.SettingsRepository

class SetThemeModeUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}
