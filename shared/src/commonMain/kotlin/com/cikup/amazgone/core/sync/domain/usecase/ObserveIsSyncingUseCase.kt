package com.cikup.amazgone.core.sync.domain.usecase

import com.cikup.amazgone.core.sync.domain.SyncEngine
import kotlinx.coroutines.flow.StateFlow

class ObserveIsSyncingUseCase(private val engine: SyncEngine) {
    operator fun invoke(): StateFlow<Boolean> = engine.isSyncing
}
