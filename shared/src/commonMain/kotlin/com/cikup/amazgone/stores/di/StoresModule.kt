package com.cikup.amazgone.stores.di

import com.cikup.amazgone.stores.data.RoomStoreRepository
import com.cikup.amazgone.stores.domain.repository.StoreRepository
import com.cikup.amazgone.stores.domain.usecase.ObserveStorePageUseCase
import com.cikup.amazgone.stores.domain.usecase.ObserveStoresUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val storesModule = module {
    singleOf(::RoomStoreRepository) bind StoreRepository::class
    factoryOf(::ObserveStoresUseCase)
    factoryOf(::ObserveStorePageUseCase)
}
