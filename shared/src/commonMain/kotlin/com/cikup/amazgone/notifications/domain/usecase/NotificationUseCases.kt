package com.cikup.amazgone.notifications.domain.usecase

import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Inbox = notifications already due at [now] (future ones stay hidden until their moment). */
class ObserveInboxUseCase(private val notifications: NotificationRepository) {
    operator fun invoke(now: Flow<Long>): Flow<List<AppNotification>> =
        combine(notifications.observeAll(), now) { all, at -> all.filter { it.deliverAt <= at } }
}

class MarkNotificationReadUseCase(private val notifications: NotificationRepository) {
    suspend operator fun invoke(id: String) = notifications.markRead(id)
}

class MarkAllNotificationsReadUseCase(private val notifications: NotificationRepository) {
    suspend operator fun invoke(upTo: Long) = notifications.markAllRead(upTo)
}

class RemoveNotificationUseCase(private val notifications: NotificationRepository) {
    suspend operator fun invoke(id: String) = notifications.remove(id)
}
