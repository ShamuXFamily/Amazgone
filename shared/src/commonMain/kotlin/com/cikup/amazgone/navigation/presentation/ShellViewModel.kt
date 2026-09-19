package com.cikup.amazgone.navigation.presentation

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.account.domain.usecase.ObserveSessionUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartCountUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.usecase.MarkNotificationReadUseCase
import com.cikup.amazgone.notifications.domain.usecase.ObserveInboxUseCase
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import com.cikup.amazgone.progress.domain.usecase.TrackAchievementsUseCase

class ShellViewModel(
    observeCartCount: ObserveCartCountUseCase,
    observeProgress: ObserveProgressUseCase,
    trackAchievements: TrackAchievementsUseCase,
    observeSession: ObserveSessionUseCase,
    observeInbox: ObserveInboxUseCase,
    ticker: TickerUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val analytics: Analytics,
) : MviViewModel<ShellState, ShellIntent, ShellEffect>(ShellState()) {

    private var lastLevel: Int? = null
    /** Inbox ids already known; null until the first emission (which is history, not news). */
    private var seenIds: Set<String>? = null

    init {
        observeCartCount().observe { setState { copy(cartCount = it) } }
        trackAchievements().observe { unlocked ->
            unlocked.forEach { analytics.unlockAchievement(it.name) }
            sendEffect(ShellEffect.AchievementsUnlocked(unlocked))
        }
        observeSession().observe { analytics.identify(it?.uid) }
        observeProgress().observe { progress ->
            val previous = lastLevel
            lastLevel = progress.level
            // the first emission is the starting level, not a level-up
            if (previous != null && progress.level > previous) {
                analytics.levelUp(progress.level)
                setState { copy(levelUpTo = progress.level) }
            }
        }
        observeInbox(ticker(INBOX_TICK_MS)).observe(::onInbox)
    }

    private fun onInbox(items: List<AppNotification>) {
        val previous = seenIds
        seenIds = items.mapTo(HashSet()) { it.id }
        if (previous == null) return
        val fresh = items.filter { !it.read && it.id !in previous }.maxByOrNull { it.deliverAt } ?: return
        setState { copy(banner = fresh) }
    }

    override fun handleIntent(intent: ShellIntent) = when (intent) {
        ShellIntent.DismissLevelUp -> setState { copy(levelUpTo = null) }
        ShellIntent.DismissBanner -> setState { copy(banner = null) }
        ShellIntent.OpenBanner -> {
            currentState.banner?.let { banner ->
                launchSafely { markRead(banner.id) }
                sendEffect(ShellEffect.OpenLink(banner.link))
            }
            setState { copy(banner = null) }
        }
    }

    private companion object {
        const val INBOX_TICK_MS = 5_000L
    }
}
