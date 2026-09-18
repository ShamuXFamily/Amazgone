package com.cikup.amazgone.core.sync

import com.cikup.amazgone.core.sync.domain.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Called from Swift's BGAppRefreshTask handler. Returns a cancel callback for the expiration handler. */
object BackgroundSync : KoinComponent {
    private val engine: SyncEngine by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun run(onComplete: (Boolean) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            val ok = runCatching { engine.sync() }.isSuccess
            onComplete(ok)
        }
        return { job.cancel() }
    }
}
