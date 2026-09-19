package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.database.databaseBuilder
import com.cikup.amazgone.core.notifications.IosSystemNotifications
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import com.cikup.amazgone.core.remote.FirebaseConfigHolder
import com.cikup.amazgone.core.remote.loadFirebaseConfig
import com.cikup.amazgone.core.storage.KeychainSecureStore
import com.cikup.amazgone.core.storage.SecureStore
import com.cikup.amazgone.core.sync.IosConnectivityObserver
import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

actual val platformModule = module {
    single { databaseBuilder() }
    single<ConnectivityObserver> { IosConnectivityObserver() }
    single<HttpClientEngine> { Darwin.create() }
    single { FirebaseConfigHolder(loadFirebaseConfig()) }
    single<SecureStore> { KeychainSecureStore(service = "com.cikup.amazgone.auth") }
    single<SystemNotifications> { IosSystemNotifications() }
}
