package com.cikup.amazgone.core.di

import com.cikup.amazgone.core.common.ApplicationScope
import com.cikup.amazgone.core.database.databaseBuilder
import com.cikup.amazgone.core.remote.FirebaseConfigHolder
import com.cikup.amazgone.core.remote.loadFirebaseConfig
import com.cikup.amazgone.core.storage.EncryptedSecureStore
import com.cikup.amazgone.core.storage.SecureStore
import com.cikup.amazgone.core.sync.AndroidConnectivityObserver
import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { databaseBuilder(androidContext()) }
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext(), get<ApplicationScope>().scope) }
    single<HttpClientEngine> { OkHttp.create() }
    single { FirebaseConfigHolder(loadFirebaseConfig(androidContext())) }
    single<SecureStore> { EncryptedSecureStore(androidContext()) }
}
