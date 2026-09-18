package com.cikup.amazgone.core.sync.domain.usecase

import com.cikup.amazgone.core.sync.domain.SyncCoordinator

/** User-initiated refresh (pull-to-refresh, retry button). */
class RequestSyncUseCase(private val coordinator: SyncCoordinator) {
    operator fun invoke(force: Boolean = true) = coordinator.requestSync(force)
}
