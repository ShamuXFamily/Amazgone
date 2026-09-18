package com.cikup.amazgone.account.di

import com.cikup.amazgone.account.data.remote.AuthRepositoryImpl
import com.cikup.amazgone.account.data.repository.ProfileRepositoryImpl
import com.cikup.amazgone.account.data.sync.ProfileCreateHandler
import com.cikup.amazgone.account.data.sync.ProfilePuller
import com.cikup.amazgone.account.data.sync.SessionSyncGate
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.account.domain.repository.ProfileRepository
import com.cikup.amazgone.account.domain.usecase.LoginUseCase
import com.cikup.amazgone.account.domain.usecase.LogoutUseCase
import com.cikup.amazgone.account.domain.usecase.ObserveProfileUseCase
import com.cikup.amazgone.account.domain.usecase.ObserveSessionUseCase
import com.cikup.amazgone.account.domain.usecase.RegisterUseCase
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.remote.FirebaseAuthApi
import com.cikup.amazgone.core.remote.FirebaseConfigHolder
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.remote.FirestoreClient
import com.cikup.amazgone.core.remote.IdTokenProvider
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.core.sync.domain.SyncGate
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val accountModule = module {
    single {
        val config = get<FirebaseConfigHolder>().config
        AuthRepositoryImpl(config?.let { FirebaseAuthApi(get(), it) }, get(), get(), get())
    } binds arrayOf(AuthRepository::class, IdTokenProvider::class)
    single {
        val config = get<FirebaseConfigHolder>().config
        FirebaseServices(
            auth = config?.let { FirebaseAuthApi(get(), it) },
            firestore = config?.let { FirestoreClient(get(), it, get<IdTokenProvider>()) },
        )
    }
    single { get<AppDatabase>().userProfileDao() }
    singleOf(::ProfileRepositoryImpl) binds arrayOf(ProfileRepository::class, UserScopedStore::class)
    singleOf(::SessionSyncGate) bind SyncGate::class
    singleOf(::ProfileCreateHandler) bind OutboxHandler::class
    singleOf(::ProfilePuller) bind RemotePuller::class

    factoryOf(::RegisterUseCase)
    factory { LoginUseCase(get(), getAll()) }
    factory { LogoutUseCase(get(), get(), getAll()) }
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ObserveProfileUseCase)
}
