package com.cikup.amazgone.core.sync

import com.cikup.amazgone.core.sync.domain.Backoff
import com.cikup.amazgone.core.sync.domain.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class BackoffAndStatusTest {
    @Test
    fun backoffDoublesFromBase() {
        assertEquals(0L, Backoff.delayMillis(0))
        assertEquals(Backoff.BASE_MILLIS, Backoff.delayMillis(1))
        assertEquals(Backoff.BASE_MILLIS * 4, Backoff.delayMillis(3))
    }

    @Test
    fun backoffIsCapped() {
        assertEquals(Backoff.MAX_MILLIS, Backoff.delayMillis(100))
    }

    @Test
    fun statusReflectsConnectivityFirst() {
        assertEquals(SyncStatus.Offline(3), SyncStatus.from(isOnline = false, pendingCount = 3, lastError = "x"))
    }

    @Test
    fun statusShowsErrorOnlyWhenWorkIsStuck() {
        assertEquals(SyncStatus.Error(2, "boom"), SyncStatus.from(true, 2, "boom"))
        assertEquals(SyncStatus.Synced, SyncStatus.from(true, 0, "stale error"))
        assertEquals(SyncStatus.Pending(1), SyncStatus.from(true, 1, null))
    }
}
