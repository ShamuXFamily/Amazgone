package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.core.analytics.AnalyticsSink
import com.cikup.amazgone.core.analytics.NoopAnalyticsSink
import com.cikup.amazgone.core.common.AppDispatchers
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.AppStartup
import com.cikup.amazgone.core.common.ApplicationScope
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.common.SystemTimeProvider
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.common.UuidGenerator
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.database.RoomTransactionRunner
import com.cikup.amazgone.core.database.TransactionRunner
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.network.createHttpClient
import com.cikup.amazgone.core.sync.data.RoomOutboxStore
import com.cikup.amazgone.core.sync.domain.OutboxStore
import com.cikup.amazgone.core.sync.domain.SyncCoordinator
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.core.sync.domain.usecase.ObserveSyncStatusUseCase
import com.cikup.amazgone.core.sync.domain.usecase.ObserveIsSyncingUseCase
import com.cikup.amazgone.core.sync.domain.usecase.RequestSyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val coreModule = module {
    single<TimeProvider> { SystemTimeProvider }
    single<IdGenerator> { UuidGenerator }
    single<AppLogger> { PrintLogger }
    single { AppDispatchers() }
    single { Analytics(getOrNull<AnalyticsSink>() ?: NoopAnalyticsSink, get()) }
    single { ApplicationScope(CoroutineScope(SupervisorJob() + get<AppDispatchers>().default)) }

    single { AppDatabase.build(get(), get<AppDispatchers>().io) }
    single { get<AppDatabase>().outboxDao() }
    single { get<AppDatabase>().syncMetaDao() }
    singleOf(::RoomTransactionRunner) bind TransactionRunner::class

    single { createHttpClient(get()) }
    singleOf(::RoomOutboxStore) binds arrayOf(OutboxStore::class, UserScopedStore::class)
    single {
        SyncEngine(
            store = get(),
            handlers = getAll(),
            pullers = getAll(),
            gate = get(),
            connectivity = get(),
            time = get(),
            logger = get(),
        )
    }
    singleOf(::SyncCoordinator)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::RequestSyncUseCase)
    factoryOf(::ObserveIsSyncingUseCase)

    single {
        val coordinator = get<SyncCoordinator>()
        AppStartup(get(), getAll(), onReady = { scope -> coordinator.start(scope) }, logger = get())
    }
}
