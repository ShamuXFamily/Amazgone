package com.cikup.amazgone.notifications.domain.repository

import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.model.PlannedNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Every stored notification, newest first, including ones still scheduled for the future. */
    fun observeAll(): Flow<List<AppNotification>>
    suspend fun markRead(id: String)
    suspend fun markAllRead(upTo: Long)
    suspend fun remove(id: String)

    /** Stores new planned notifications (and schedules future ones); drops future ones that are no longer planned. */
    suspend fun reconcile(plan: List<PlannedNotification>, now: Long)

    /** When notifications were first switched on; older events never notify. */
    suspend fun since(): Long
    suspend fun lastDeliverAt(kindPrefix: String): Long?
}

/** The operating system's notification centre (iOS UNUserNotificationCenter, Android WorkManager + NotificationManager). */
interface SystemNotifications {
    fun schedule(id: String, title: String, body: String, atMillis: Long, link: String)
    fun cancel(id: String)
}

/** Turns a planned notification into localized title + body (implemented in presentation with string resources). */
interface NotificationRenderer {
    suspend fun render(notification: PlannedNotification): Pair<String, String>
}
