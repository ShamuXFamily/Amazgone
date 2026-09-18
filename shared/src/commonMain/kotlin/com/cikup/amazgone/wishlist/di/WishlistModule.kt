package com.cikup.amazgone.wishlist.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.core.sync.domain.RemotePuller
import com.cikup.amazgone.wishlist.data.WishlistPuller
import com.cikup.amazgone.wishlist.data.WishlistRepositoryImpl
import com.cikup.amazgone.wishlist.data.WishlistSetHandler
import com.cikup.amazgone.wishlist.domain.repository.WishlistRepository
import com.cikup.amazgone.wishlist.domain.usecase.ObserveIsSavedUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistIdsUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistUseCase
import com.cikup.amazgone.wishlist.domain.usecase.SaveToWishlistUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val wishlistModule = module {
    single { get<AppDatabase>().wishlistDao() }
    singleOf(::WishlistRepositoryImpl) binds arrayOf(WishlistRepository::class, UserScopedStore::class)
    singleOf(::WishlistSetHandler) bind OutboxHandler::class
    singleOf(::WishlistPuller) bind RemotePuller::class
    factoryOf(::ToggleWishlistUseCase)
    factoryOf(::SaveToWishlistUseCase)
    factoryOf(::ObserveWishlistUseCase)
    factoryOf(::ObserveIsSavedUseCase)
    factoryOf(::ObserveWishlistIdsUseCase)
}
