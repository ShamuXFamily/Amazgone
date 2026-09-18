package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.common.AppStartup
import com.cikup.amazgone.account.di.accountModule
import com.cikup.amazgone.cart.di.cartModule
import com.cikup.amazgone.catalog.di.catalogModule
import com.cikup.amazgone.games.di.gamesModule
import com.cikup.amazgone.orders.di.ordersModule
import com.cikup.amazgone.progress.di.progressModule
import com.cikup.amazgone.wallet.di.walletModule
import com.cikup.amazgone.wishlist.di.wishlistModule
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** Platform bindings: database builder, connectivity, HTTP engine. */
expect val platformModule: Module

/** Feature modules are appended here as each feature lands. */
internal val appModules: List<Module>
    get() = listOf(platformModule, coreModule) + featureModules

internal val featureModules: List<Module> = listOf(
    accountModule,
    walletModule,
    catalogModule,
    cartModule,
    ordersModule,
    wishlistModule,
    gamesModule,
    progressModule,
    presentationModule,
)

/** Starts DI once per process, then kicks off startup tasks and the sync coordinator. */
fun initKoin(platformConfig: KoinAppDeclaration = {}) {
    val koin = org.koin.core.context.startKoin {
        platformConfig()
        modules(appModules)
    }.koin
    koin.get<AppStartup>().start()
}
