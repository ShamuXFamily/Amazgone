package com.cikup.amazgone.core.sync.domain

sealed interface SyncStatus {
    data object Synced : SyncStatus
    data class Pending(val count: Int) : SyncStatus
    data class Offline(val pendingCount: Int) : SyncStatus
    data class Error(val pendingCount: Int, val message: String) : SyncStatus

    companion object {
        fun from(isOnline: Boolean, pendingCount: Int, lastError: String?): SyncStatus = when {
            !isOnline -> Offline(pendingCount)
            lastError != null && pendingCount > 0 -> Error(pendingCount, lastError)
            pendingCount > 0 -> Pending(pendingCount)
            else -> Synced
        }
    }
}
