package com.cikup.amazgone.reviews.di

import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.domain.UserScopedStore
import com.cikup.amazgone.core.sync.domain.OutboxHandler
import com.cikup.amazgone.reviews.data.ReviewRepositoryImpl
import com.cikup.amazgone.reviews.data.ReviewSaveHandler
import com.cikup.amazgone.reviews.domain.repository.ReviewRepository
import com.cikup.amazgone.reviews.domain.usecase.ObserveMyReviewsUseCase
import com.cikup.amazgone.reviews.domain.usecase.ObserveProductReviewsUseCase
import com.cikup.amazgone.reviews.domain.usecase.RefreshProductReviewsUseCase
import com.cikup.amazgone.reviews.domain.usecase.SubmitReviewUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val reviewsModule = module {
    single { get<AppDatabase>().shopperReviewDao() }
    singleOf(::ReviewRepositoryImpl) binds arrayOf(ReviewRepository::class, UserScopedStore::class)
    singleOf(::ReviewSaveHandler) bind OutboxHandler::class
    factoryOf(::ObserveProductReviewsUseCase)
    factoryOf(::ObserveMyReviewsUseCase)
    factoryOf(::RefreshProductReviewsUseCase)
    factoryOf(::SubmitReviewUseCase)
}
