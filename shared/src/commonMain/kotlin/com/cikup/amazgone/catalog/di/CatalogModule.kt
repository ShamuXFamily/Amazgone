package com.cikup.amazgone.catalog.di

import amazgone.shared.generated.resources.Res
import com.cikup.amazgone.catalog.data.remote.CatalogSource
import com.cikup.amazgone.catalog.data.remote.CheapSharkSource
import com.cikup.amazgone.catalog.data.remote.CuratedCatalogSource
import com.cikup.amazgone.catalog.data.remote.DummyJsonSource
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.catalog.data.sync.CatalogSyncer
import com.cikup.amazgone.catalog.data.sync.SeedCatalogImporter
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import com.cikup.amazgone.catalog.domain.usecase.ObserveCategoriesUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveFlashSaleUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveHomeFeedUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveProductDetailUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveRecommendationsUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchProductsUseCase
import com.cikup.amazgone.catalog.domain.usecase.SearchRemoteCatalogUseCase
import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.sync.domain.RemotePuller
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

@OptIn(ExperimentalResourceApi::class)
val catalogModule = module {
    single { get<AppDatabase>().catalogDao() }
    single(named("dummyjson")) { DummyJsonSource(get(), get()) } bind CatalogSource::class
    single(named("cheapshark")) { CheapSharkSource(get(), get()) } bind CatalogSource::class
    single(named("curated")) { CuratedCatalogSource(get(), get()) } bind CatalogSource::class
    single { CatalogRepositoryImpl(get(), getAll(), get(), get()) } binds arrayOf(CatalogRepository::class)
    single { CatalogSyncer(getAll(), get(), get(), get(), get()) } bind RemotePuller::class
    single(named("seed-catalog")) {
        SeedCatalogImporter(get()) { path -> Res.readBytes(path) }
    } bind StartupTask::class

    factoryOf(::ObserveHomeFeedUseCase)
    factoryOf(::SearchProductsUseCase)
    factoryOf(::SearchRemoteCatalogUseCase)
    factoryOf(::ObserveProductDetailUseCase)
    factoryOf(::ObserveRecommendationsUseCase)
    factoryOf(::ObserveCategoriesUseCase)
    factoryOf(::ObserveFlashSaleUseCase)
}
