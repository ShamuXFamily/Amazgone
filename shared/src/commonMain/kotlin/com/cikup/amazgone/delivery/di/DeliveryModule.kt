package com.cikup.amazgone.delivery.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.delivery.data.CourierBuyHandler
import com.cikup.amazgone.delivery.data.CourierRepositoryImpl
import com.cikup.amazgone.delivery.data.CouriersPuller
import com.cikup.amazgone.delivery.data.NominatimGeocoder
import com.cikup.amazgone.delivery.domain.repository.CourierRepository
import com.cikup.amazgone.delivery.domain.repository.Geocoder
import com.cikup.amazgone.delivery.domain.usecase.BuyCourierUseCase
import com.cikup.amazgone.delivery.domain.usecase.LocateAddressUseCase
import com.cikup.amazgone.delivery.domain.usecase.ObserveOwnedCouriersUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val deliveryModule = module {
    single { get<AppDatabase>().ownedCourierDao() }
    singleOf(::CourierRepositoryImpl) binds arrayOf(CourierRepository::class, UserScopedStore::class)
    singleOf(::CourierBuyHandler) bind OutboxHandler::class
    singleOf(::CouriersPuller) bind RemotePuller::class
    single<Geocoder> { NominatimGeocoder(get(), get()) }
    factoryOf(::ObserveOwnedCouriersUseCase)
    factoryOf(::BuyCourierUseCase)
    factoryOf(::LocateAddressUseCase)
}
