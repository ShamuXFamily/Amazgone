package com.cikup.amazgone.cart.di

import com.cikup.amazgone.cart.data.coupon.CouponRepositoryImpl
import com.cikup.amazgone.cart.data.repository.CartRepositoryImpl
import com.cikup.amazgone.cart.data.sync.CartPuller
import com.cikup.amazgone.cart.data.sync.CartSetHandler
import com.cikup.amazgone.cart.domain.repository.CouponRepository
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.cart.domain.repository.CartRepository
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartCountUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCouponsUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveQuantityInCartUseCase
import com.cikup.amazgone.cart.domain.usecase.RestoreCartUseCase
import com.cikup.amazgone.cart.domain.usecase.UpdateCartQuantityUseCase
import com.cikup.amazgone.core.database.AppDatabase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val cartModule = module {
    single { get<AppDatabase>().cartDao() }
    single { get<AppDatabase>().couponDao() }
    singleOf(::CartRepositoryImpl) bind CartRepository::class
    singleOf(::CouponRepositoryImpl) binds arrayOf(CouponRepository::class, UserScopedStore::class)
    singleOf(::CartSetHandler) bind OutboxHandler::class
    singleOf(::CartPuller) bind RemotePuller::class
    factoryOf(::ObserveCartUseCase)
    factoryOf(::ObserveCartCountUseCase)
    factoryOf(::AddToCartUseCase)
    factoryOf(::UpdateCartQuantityUseCase)
    factoryOf(::RestoreCartUseCase)
    factoryOf(::ObserveQuantityInCartUseCase)
    factoryOf(::ObserveCouponsUseCase)
}
