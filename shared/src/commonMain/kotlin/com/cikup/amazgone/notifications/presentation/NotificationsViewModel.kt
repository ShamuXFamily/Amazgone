package com.cikup.amazgone.notifications.presentation

import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.notifications.domain.usecase.MarkAllNotificationsReadUseCase
import com.cikup.amazgone.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cikup.amazgone.notifications.domain.usecase.ObserveInboxUseCase
import com.cikup.amazgone.notifications.domain.usecase.RemoveNotificationUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class NotificationsViewModel(
    observeInbox: ObserveInboxUseCase,
    ticker: TickerUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val markAllRead: MarkAllNotificationsReadUseCase,
    private val remove: RemoveNotificationUseCase,
) : MviViewModel<NotificationsState, NotificationsIntent, NotificationsEffect>(NotificationsState()) {

    init {
        val ticks = ticker(TICK_MS).shareIn(vmScope, SharingStarted.WhileSubscribed(), replay = 1)
        ticks.observe { setState { copy(now = it) } }
        observeInbox(ticks).observe { setState { copy(items = it, isLoading = false) } }
    }

    override fun handleIntent(intent: NotificationsIntent) {
        when (intent) {
            is NotificationsIntent.Open -> {
                val item = currentState.items.firstOrNull { it.id == intent.id } ?: return
                launchSafely { markRead(item.id) }
                sendEffect(NotificationsEffect.OpenLink(item.link))
            }
            is NotificationsIntent.Remove -> launchSafely { remove(intent.id) }
            NotificationsIntent.MarkAllRead -> launchSafely { markAllRead(currentState.now) }
            NotificationsIntent.Back -> sendEffect(NotificationsEffect.NavigateBack)
        }
    }

    private companion object {
        const val TICK_MS = 15_000L
    }
}
