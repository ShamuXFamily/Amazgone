package com.cikup.amazgone.testing

import com.cikup.amazgone.core.common.AppDispatchers
import com.cikup.amazgone.core.common.IdGenerator
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.database.AppDatabase
import com.cikup.amazgone.core.database.inMemoryDatabase
import com.cikup.amazgone.core.di.coreModule
import com.cikup.amazgone.core.di.featureModules
import com.cikup.amazgone.core.remote.FirebaseConfigHolder
import com.cikup.amazgone.core.storage.InMemorySecureStore
import com.cikup.amazgone.core.storage.SecureStore
import com.cikup.amazgone.core.sync.domain.ConnectivityObserver
import com.cikup.amazgone.remote.FakeFirebase
import com.cikup.amazgone.remote.TEST_CONFIG
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The real production Koin graph with only the platform swapped for test doubles:
 * in-memory Room, scripted Firebase REST backend, in-memory secure store, controllable clock/ids.
 */
class TestGraph(
    val clock: FakeClock = FakeClock(),
    val backend: FakeFirebase = FakeFirebase(),
    val connectivity: FakeConnectivity = FakeConnectivity(online = true),
    val secureStore: SecureStore = InMemorySecureStore(),
    firebaseConfigured: Boolean = true,
) {
    val database: AppDatabase = inMemoryDatabase()

    private val testPlatform = module {
        single<HttpClientEngine> { backend.engine }
        single { FirebaseConfigHolder(if (firebaseConfigured) TEST_CONFIG else null) }
        single<SecureStore> { secureStore }
        single<ConnectivityObserver> { connectivity }
    }

    private val overrides = module {
        single<TimeProvider> { clock }
        single<IdGenerator> { SequentialIds() }
        single { AppDispatchers(io = Dispatchers.Default) }
        single { database }
    }

    private val app: KoinApplication = koinApplication {
        allowOverride(true)
        modules(listOf(testPlatform, coreModule) + featureModules + overrides)
    }

    val koin: Koin get() = app.koin

    inline fun <reified T : Any> get(): T = koin.get()

    /** What AppStartup does on first launch: the guest starter coins. */
    suspend fun grantGuestWallet() = get<WalletRepositoryImpl>().run()

    /**
     * Stops DI only. The in-memory database is deliberately left open: ViewModels under test keep
     * ticking on their own scopes, and closing SQLite underneath them segfaults on Kotlin/Native.
     * Each test gets a fresh tiny in-memory DB, released when the test process exits.
     */
    fun close() {
        app.close()
    }
}

/** Waits (in real time, since Room emits on real threads) until [predicate] holds. */
suspend fun <T> Flow<T>.eventually(timeoutMs: Long = 5_000, predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default) { withTimeout(timeoutMs) { first(predicate) } }

/**
 * Real (single-threaded) Main for ViewModels under test. A TestDispatcher would share runTest's
 * scheduler, and runTest would then try to drain the ViewModels' endless countdown tickers.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
val mainForViewModels: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
