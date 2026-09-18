package com.cikup.amazgone.progress.di

import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.progress.data.AchievementRepositoryImpl
import com.cikup.amazgone.progress.data.AchievementUnlockHandler
import com.cikup.amazgone.progress.data.AchievementsPuller
import com.cikup.amazgone.progress.data.DailyVisitTask
import com.cikup.amazgone.progress.data.DailyXpHandler
import com.cikup.amazgone.progress.data.LeaderboardPuller
import com.cikup.amazgone.progress.data.LeaderboardRepositoryImpl
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import com.cikup.amazgone.progress.domain.repository.LeaderboardRepository
import com.cikup.amazgone.progress.domain.usecase.ObserveAchievementsUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveLeaderboardUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import com.cikup.amazgone.progress.domain.usecase.TrackAchievementsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val progressModule = module {
    single { get<AppDatabase>().achievementDao() }
    single { get<AppDatabase>().leaderboardDao() }
    singleOf(::AchievementRepositoryImpl) binds arrayOf(AchievementRepository::class, UserScopedStore::class)
    singleOf(::LeaderboardRepositoryImpl) binds arrayOf(LeaderboardRepository::class, UserScopedStore::class)
    singleOf(::LeaderboardPuller) bind RemotePuller::class
    singleOf(::AchievementsPuller) bind RemotePuller::class
    singleOf(::AchievementUnlockHandler) bind OutboxHandler::class
    singleOf(::DailyXpHandler) bind OutboxHandler::class
    singleOf(::DailyVisitTask) bind StartupTask::class

    factoryOf(::ObserveProgressUseCase)
    factoryOf(::ObserveAchievementsUseCase)
    factoryOf(::TrackAchievementsUseCase)
    factoryOf(::ObserveLeaderboardUseCase)
}
