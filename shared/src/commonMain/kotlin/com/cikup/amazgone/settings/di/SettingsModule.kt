package com.cikup.amazgone.settings.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.settings.data.RoomSettingsRepository
import com.cikup.amazgone.settings.domain.repository.SettingsRepository
import com.cikup.amazgone.settings.domain.usecase.ObserveThemeModeUseCase
import com.cikup.amazgone.settings.domain.usecase.SetThemeModeUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
    single { get<AppDatabase>().settingsDao() }
    singleOf(::RoomSettingsRepository) bind SettingsRepository::class
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
}
