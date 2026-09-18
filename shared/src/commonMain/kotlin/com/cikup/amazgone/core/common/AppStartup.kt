package com.cikup.amazgone.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Process-wide scope for work that must outlive any screen (sync, seeding). */
class ApplicationScope(val scope: CoroutineScope)

/** One-time work at launch (e.g. importing the bundled seed catalog). */
interface StartupTask {
    val name: String
    suspend fun run()
}

class AppStartup(
    private val appScope: ApplicationScope,
    private val tasks: List<StartupTask>,
    private val onReady: (CoroutineScope) -> Unit,
    private val logger: AppLogger,
) {
    fun start() {
        appScope.scope.launch {
            tasks.forEach { task ->
                runCatching { task.run() }
                    .onFailure { logger.error("AppStartup", "Startup task '${task.name}' failed", it) }
            }
            onReady(appScope.scope)
        }
    }
}
