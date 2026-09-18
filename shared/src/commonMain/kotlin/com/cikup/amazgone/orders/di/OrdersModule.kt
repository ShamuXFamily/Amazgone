package com.cikup.amazgone.orders.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.orders.data.OrderPlaceHandler
import com.cikup.amazgone.orders.data.OrderRepositoryImpl
import com.cikup.amazgone.orders.data.OrdersPuller
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.orders.domain.usecase.ObserveOrderUseCase
import com.cikup.amazgone.orders.domain.usecase.ObserveOrdersUseCase
import com.cikup.amazgone.orders.domain.usecase.PlaceOrderUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val ordersModule = module {
    single { get<AppDatabase>().orderDao() }
    singleOf(::OrderRepositoryImpl) binds arrayOf(OrderRepository::class, UserScopedStore::class)
    singleOf(::OrderPlaceHandler) bind OutboxHandler::class
    singleOf(::OrdersPuller) bind RemotePuller::class
    factoryOf(::PlaceOrderUseCase)
    factoryOf(::ObserveOrdersUseCase)
    factoryOf(::ObserveOrderUseCase)
}
