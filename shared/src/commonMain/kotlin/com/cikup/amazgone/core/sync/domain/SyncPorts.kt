package com.cikup.amazgone.core.sync.domain

import kotlinx.coroutines.flow.StateFlow

/** Network reachability, implemented per platform (ConnectivityManager / NWPathMonitor). */
interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
}

/** Pushing requires a signed-in account; guests keep their outbox until they register. */
fun interface SyncGate {
    suspend fun canPush(): Boolean
}

/** Refreshes a slice of local data from a remote source (catalog, profile, leaderboard…). */
interface RemotePuller {
    val name: String
    /** Whether this puller needs a signed-in account. */
    val requiresAuth: Boolean
    suspend fun pull(force: Boolean)
}
