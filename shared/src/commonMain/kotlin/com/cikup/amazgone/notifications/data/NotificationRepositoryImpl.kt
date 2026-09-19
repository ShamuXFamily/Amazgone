package com.cikup.amazgone.notifications.data

import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.PlannedNotification
import com.cikup.amazgone.notifications.domain.repository.NotificationRenderer
import com.cikup.amazgone.notifications.domain.repository.NotificationRepository
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import com.cikup.amazgone.settings.data.SettingEntity
import com.cikup.amazgone.settings.data.SettingsDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationRepositoryImpl(
    private val dao: NotificationDao,
    private val settings: SettingsDao,
    private val renderer: NotificationRenderer,
    private val system: SystemNotifications,
    private val time: TimeProvider,
    private val logger: AppLogger,
) : NotificationRepository {
    private val mutex = Mutex()

    override fun observeAll(): Flow<List<AppNotification>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun markRead(id: String) = dao.markRead(id)

    override suspend fun markAllRead(upTo: Long) = dao.markAllRead(upTo)

    override suspend fun remove(id: String) {
        dao.delete(listOf(id))
        system.cancel(id)
    }

    override suspend fun reconcile(plan: List<PlannedNotification>, now: Long) = mutex.withLock {
        val planned = plan.associateBy { it.id }
        // Future notifications whose event went away (e.g. an order was refused) are withdrawn.
        val stale = dao.futureIds(now).filterNot { it in planned }
        if (stale.isNotEmpty()) {
            dao.delete(stale)
            stale.forEach(system::cancel)
        }
        val known = dao.ids().toSet()
        val fresh = plan.filterNot { it.id in known }
        if (fresh.isEmpty()) return@withLock
        val rows = fresh.map { p ->
            val (title, body) = renderer.render(p)
            NotificationEntity(p.id, p.kind.name, title, body, p.link, p.deliverAt, read = false)
        }
        dao.insert(rows)
        rows.filter { it.deliverAt > now }.forEach { row ->
            try {
                system.schedule(row.id, row.title, row.body, row.deliverAt, row.link)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                logger.error(TAG, "Scheduling ${row.id} failed; it will still appear in the inbox", t)
            }
        }
    }

    override suspend fun since(): Long {
        settings.observe(KEY_SINCE).first()?.toLongOrNull()?.let { return it }
        val now = time.nowMillis()
        settings.upsert(SettingEntity(KEY_SINCE, now.toString()))
        return now
    }

    override suspend fun lastDeliverAt(kindPrefix: String): Long? = dao.lastDeliverAt(kindPrefix)

    private companion object {
        const val TAG = "Notifications"
        const val KEY_SINCE = "notifications_since"
    }
}

private fun NotificationEntity.toDomain(): AppNotification? {
    val kind = NotificationKind.entries.firstOrNull { it.name == this.kind } ?: return null
    return AppNotification(id, kind, title, body, link, deliverAt, read)
}
