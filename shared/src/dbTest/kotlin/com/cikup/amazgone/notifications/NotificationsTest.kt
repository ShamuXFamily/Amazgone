package com.cikup.amazgone.notifications

import com.cikup.amazgone.core.common.StartupTask
import com.cikup.amazgone.games.presentation.spin.SpinIntent
import com.cikup.amazgone.games.presentation.spin.SpinViewModel
import com.cikup.amazgone.navigation.presentation.ShellEffect
import com.cikup.amazgone.navigation.presentation.ShellIntent
import com.cikup.amazgone.navigation.presentation.ShellViewModel
import com.cikup.amazgone.notifications.data.NotificationRepositoryImpl
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.NotificationLinks
import com.cikup.amazgone.notifications.domain.model.PlannedNotification
import com.cikup.amazgone.notifications.domain.repository.NotificationRenderer
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import com.cikup.amazgone.notifications.presentation.NotificationsEffect
import com.cikup.amazgone.notifications.presentation.NotificationsIntent
import com.cikup.amazgone.notifications.presentation.NotificationsViewModel
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.mainForViewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingSystem : SystemNotifications {
    val scheduled = mutableListOf<Pair<String, Long>>()
    val cancelled = mutableListOf<String>()
    override fun schedule(id: String, title: String, body: String, atMillis: Long, link: String) {
        scheduled += id to atMillis
    }
    override fun cancel(id: String) {
        cancelled += id
    }
}

/** Plain text so the tests don't depend on Compose resources. */
private object PlainRenderer : NotificationRenderer {
    override suspend fun render(notification: PlannedNotification) =
        notification.kind.name to notification.args.joinToString()
}

class NotificationsTest {
    private lateinit var graph: TestGraph
    private val system = RecordingSystem()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainForViewModels)
        graph = TestGraph()
        graph.koin.loadModules(
            listOf(module { single<SystemNotifications> { system }; single<NotificationRenderer> { PlainRenderer } }),
            allowOverride = true,
        )
    }

    @AfterTest
    fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    private val repo get() = graph.get<NotificationRepositoryImpl>()
    private val now get() = graph.clock.now

    private fun planned(id: String, at: Long, kind: NotificationKind = NotificationKind.SPIN_READY) =
        PlannedNotification(id, kind, at, NotificationLinks.SPIN, listOf(id))

    @Test
    fun reconcileStoresOnceSchedulesFutureAndWithdrawsStale() = runTest {
        val due = planned("due", now - 1)
        val later = planned("later", now + HOUR)
        repo.reconcile(listOf(due, later), now)
        repo.reconcile(listOf(due, later), now) // re-planning never duplicates
        assertEquals(listOf("later" to now + HOUR), system.scheduled)

        val rows = repo.observeAll().first()
        assertEquals(setOf("due", "later"), rows.map { it.id }.toSet())
        assertEquals("SPIN_READY" to "due", rows.first { it.id == "due" }.let { it.title to it.body })

        repo.reconcile(listOf(due), now) // the future event went away
        assertEquals(listOf("later"), system.cancelled)
        assertEquals(listOf("due"), repo.observeAll().first().map { it.id })
    }

    @Test
    fun sinceIsRememberedFromTheFirstCall() = runTest {
        val first = repo.since()
        graph.clock.advance(HOUR)
        assertEquals(first, repo.since())
    }

    @Test
    fun inboxShowsOnlyDueItemsAndMarksThemRead() = runTest {
        repo.reconcile(listOf(planned("a", now - 2), planned("b", now - 1), planned("future", now + HOUR)), now)
        val vm = graph.get<NotificationsViewModel>()
        val loaded = vm.state.eventually { !it.isLoading && it.items.size == 2 }
        assertEquals(2, loaded.unreadCount)
        assertEquals(listOf("b", "a"), loaded.items.map { it.id }) // newest first

        vm.onIntent(NotificationsIntent.Open("b"))
        assertEquals(NotificationsEffect.OpenLink(NotificationLinks.SPIN), vm.effects.first())
        vm.state.eventually { it.unreadCount == 1 }

        vm.onIntent(NotificationsIntent.MarkAllRead)
        vm.state.eventually { it.unreadCount == 0 }

        vm.onIntent(NotificationsIntent.Remove("a"))
        vm.state.eventually { it.items.map { item -> item.id } == listOf("b") }
        assertTrue("a" in system.cancelled)

        vm.onIntent(NotificationsIntent.Back)
        assertEquals(NotificationsEffect.NavigateBack, vm.effects.first())
    }

    @Test
    fun shellShowsABannerOnlyForNewArrivals() = runTest {
        repo.reconcile(listOf(planned("old", now - 1)), now)
        val shell = graph.get<ShellViewModel>()
        // give the shell its first (baseline) inbox before anything new arrives
        kotlinx.coroutines.withContext(Dispatchers.Default) { kotlinx.coroutines.delay(BASELINE_WAIT_MS) }
        assertEquals(null, shell.state.value.banner)

        repo.reconcile(listOf(planned("old", now - 1), planned("new", now)), now)
        assertEquals("new", shell.state.eventually { it.banner != null }.banner?.id)

        shell.onIntent(ShellIntent.OpenBanner)
        assertEquals(ShellEffect.OpenLink(NotificationLinks.SPIN), shell.effects.first())
        assertEquals(null, shell.state.value.banner)
        assertTrue(repo.observeAll().eventually { rows -> rows.first { it.id == "new" }.read }.isNotEmpty())
    }

    @Test
    fun coordinatorSchedulesTheNextSpinAfterPlaying() = runTest {
        graph.koin.getAll<StartupTask>().first { it.name == "notifications" }.run()
        val spin = graph.get<SpinViewModel>()
        spin.state.eventually { it.canSpin }
        spin.onIntent(SpinIntent.Spin)
        spin.state.eventually { it.targetIndex != null }
        spin.onIntent(SpinIntent.Landed)

        val rows = repo.observeAll().eventually { rows -> rows.any { it.kind == NotificationKind.SPIN_READY } }
        val ready = rows.first { it.kind == NotificationKind.SPIN_READY }
        assertTrue(ready.deliverAt > now)
        assertFalse(ready.read)
        assertTrue(system.scheduled.any { it.first == ready.id })
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val BASELINE_WAIT_MS = 300L
    }
}
