package com.cikup.amazgone.wallet.di

import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.repository.WalletRepository
import com.cikup.amazgone.wallet.domain.usecase.ObserveWalletUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

val walletModule = module {
    single { get<AppDatabase>().ledgerDao() }
    singleOf(::WalletRepositoryImpl) binds arrayOf(WalletRepository::class, UserScopedStore::class, StartupTask::class)
    factoryOf(::ObserveWalletUseCase)
}
