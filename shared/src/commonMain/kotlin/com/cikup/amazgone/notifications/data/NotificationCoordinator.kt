package com.cikup.amazgone.notifications.data

import com.cikup.amazgone.catalog.domain.model.FlashSaleClock
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.ApplicationScope
import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.delivery.domain.repository.CourierRepository
import com.cikup.amazgone.games.domain.model.GameKind
import com.cikup.amazgone.games.domain.repository.GamesRepository
import com.cikup.amazgone.notifications.domain.model.NotificationInputs
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.NotificationLinks
import com.cikup.amazgone.notifications.domain.model.NotificationPlanner
import com.cikup.amazgone.notifications.domain.model.PlannedNotification
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.progress.domain.repository.AchievementRepository
import com.cikup.amazgone.progress.domain.usecase.ObserveProgressUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Keeps the inbox and the OS notification schedule in step with the app's data: whenever orders,
 * game cooldowns, unlocks or the clock change, it re-plans (pure [NotificationPlanner]) and reconciles.
 */
class NotificationCoordinator(
    private val appScope: ApplicationScope,
    private val orders: OrderRepository,
    private val games: GamesRepository,
    private val achievements: AchievementRepository,
    private val couriers: CourierRepository,
    private val observeProgress: ObserveProgressUseCase,
    private val notifications: NotificationRepositoryImpl,
    private val time: TimeProvider,
    private val logger: AppLogger,
) : StartupTask {
    override val name = "notifications"

    /** Level-ups seen while the app runs (level → when); the first level observed is the baseline. */
    private val levelUps = MutableStateFlow<Map<Int, Long>>(emptyMap())

    override suspend fun run() {
        val since = notifications.since()
        appScope.scope.launch { trackLevels() }
        appScope.scope.launch {
            val clock = flow {
                while (true) {
                    emit(time.nowMillis())
                    delay(TICK_MS)
                }
            }
            val gameTimes = combine(games.observeLastPlayedAt(GameKind.SPIN), games.observeLastPlayedAt(GameKind.SCRATCH)) { spin, scratch ->
                listOfNotNull(spin?.let { GameKind.SPIN to it }, scratch?.let { GameKind.SCRATCH to it }).toMap()
            }
            combine(
                orders.observeOrders(),
                gameTimes,
                combine(achievements.observeUnlocked(), couriers.observePurchases(), levelUps) { a, c, l -> Triple(a, c, l) },
                clock,
            ) { orderList, played, (unlocked, bought, levels), now ->
                val plan = NotificationPlanner.plan(
                    NotificationInputs(
                        since = since,
                        now = now,
                        orders = orderList,
                        lastPlayed = played,
                        flashWindowEnd = FlashSaleClock.windowStart(now) + FlashSaleClock.WINDOW_MILLIS,
                        lastFlashReminderAt = notifications.lastDeliverAt(FLASH_PREFIX),
                        achievements = unlocked.mapKeys { it.key.name },
                        couriers = bought,
                    ),
                ) + levels.map { (level, at) -> PlannedNotification("level-$level", NotificationKind.LEVEL_UP, at, NotificationLinks.ACHIEVEMENTS, listOf(level.toString())) }
                plan to now
            }.collect { (plan, now) -> reconcileSafely(plan, now) }
        }
    }

    private suspend fun trackLevels() {
        var baseline: Int? = null
        observeProgress().collect { progress ->
            val previous = baseline
            baseline = progress.level
            if (previous != null && progress.level > previous) {
                levelUps.value = levelUps.value + (progress.level to time.nowMillis())
            }
        }
    }

    private suspend fun reconcileSafely(plan: List<PlannedNotification>, now: Long) {
        try {
            notifications.reconcile(plan, now)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logger.error(TAG, "Updating notifications failed", t)
        }
    }

    private companion object {
        const val TAG = "NotificationCoordinator"
        const val TICK_MS = 60_000L
        const val FLASH_PREFIX = "flash-"
    }
}
