package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.database.databaseBuilder
import com.cikup.amazgone.core.remote.FirebaseConfigHolder
import com.cikup.amazgone.core.storage.InMemorySecureStore
import com.cikup.amazgone.core.storage.SecureStore
import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module

/** JVM host: local-only, always "online" (used for tests and tooling, not shipped). */
actual val platformModule = module {
    single { databaseBuilder() }
    single<ConnectivityObserver> { object : ConnectivityObserver { override val isOnline = MutableStateFlow(true) } }
    single<HttpClientEngine> { OkHttp.create() }
    single { FirebaseConfigHolder(null) }
    single<SecureStore> { InMemorySecureStore() }
}
