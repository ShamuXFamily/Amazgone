package com.cikup.amazgone.games.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.games.data.GameRewardHandler
import com.cikup.amazgone.games.data.GamesRepositoryImpl
import com.cikup.amazgone.games.domain.repository.GamesRepository
import com.cikup.amazgone.games.domain.usecase.ClaimLightningDealUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveCooldownUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveLatestPlayUseCase
import com.cikup.amazgone.games.domain.usecase.ObserveLightningDealUseCase
import com.cikup.amazgone.games.domain.usecase.PlayGameUseCase
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val gamesModule = module {
    single { get<AppDatabase>().gamePlayDao() }
    singleOf(::GamesRepositoryImpl) binds arrayOf(GamesRepository::class, UserScopedStore::class)
    singleOf(::GameRewardHandler) bind OutboxHandler::class
    factory { PlayGameUseCase(get(), get(), get()) }
    factoryOf(::ObserveCooldownUseCase)
    factoryOf(::TickerUseCase)
    factoryOf(::ObserveLightningDealUseCase)
    factoryOf(::ClaimLightningDealUseCase)
    factoryOf(::ObserveLatestPlayUseCase)
}
