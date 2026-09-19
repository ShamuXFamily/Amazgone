package com.cikup.amazgone.notifications.presentation

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.notifications.domain.model.AppNotification
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class NotificationsState(
    /** Due notifications, newest first. */
    val items: List<AppNotification> = emptyList(),
    val now: Long = 0,
    val isLoading: Boolean = true,
) : UiState {
    val unreadCount: Int get() = items.count { !it.read }
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    /** Newest first, split into what arrived today (local time) and everything before. */
    fun grouped(zone: TimeZone = TimeZone.currentSystemDefault()): Pair<List<AppNotification>, List<AppNotification>> {
        if (now == 0L) return emptyList<AppNotification>() to items
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
        return items.partition { Instant.fromEpochMilliseconds(it.deliverAt).toLocalDateTime(zone).date == today }
    }
}

sealed interface NotificationsIntent : UiIntent {
    data class Open(val id: String) : NotificationsIntent
    data class Remove(val id: String) : NotificationsIntent
    data object MarkAllRead : NotificationsIntent
    data object Back : NotificationsIntent
}

sealed interface NotificationsEffect : UiEffect {
    data class OpenLink(val link: String) : NotificationsEffect
    data object NavigateBack : NotificationsEffect
}

/** Coarse "how long ago": minutes under an hour, hours under a day, then days. */
enum class AgoUnit { NOW, MINUTES, HOURS, DAYS }

fun timeAgo(deliverAt: Long, now: Long): Pair<AgoUnit, Long> {
    val minutes = (now - deliverAt).coerceAtLeast(0) / MINUTE_MS
    return when {
        minutes < 1 -> AgoUnit.NOW to 0
        minutes < MINUTES_PER_HOUR -> AgoUnit.MINUTES to minutes
        minutes < MINUTES_PER_DAY -> AgoUnit.HOURS to minutes / MINUTES_PER_HOUR
        else -> AgoUnit.DAYS to minutes / MINUTES_PER_DAY
    }
}

private const val MINUTE_MS = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24 * 60L
