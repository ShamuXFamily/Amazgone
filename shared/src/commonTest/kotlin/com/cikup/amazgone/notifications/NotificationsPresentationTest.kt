package com.cikup.amazgone.notifications

import com.cikup.amazgone.navigation.Route
import com.cikup.amazgone.navigation.routeForLink
import com.cikup.amazgone.notifications.domain.model.AppNotification
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.NotificationLinks
import com.cikup.amazgone.notifications.presentation.AgoUnit
import com.cikup.amazgone.notifications.presentation.NotificationsState
import com.cikup.amazgone.notifications.presentation.timeAgo
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationsPresentationTest {
    @Test
    fun linksMapToTheirScreens() {
        assertEquals(Route.OrderDetail("o1"), routeForLink(NotificationLinks.order("o1")))
        assertEquals(Route.SpinWheel, routeForLink(NotificationLinks.SPIN))
        assertEquals(Route.ScratchCard, routeForLink(NotificationLinks.SCRATCH))
        assertEquals(Route.FlashSale, routeForLink(NotificationLinks.FLASH_SALE))
        assertEquals(Route.Garage, routeForLink(NotificationLinks.GARAGE))
        assertEquals(Route.Achievements, routeForLink(NotificationLinks.ACHIEVEMENTS))
        assertNull(routeForLink("order/"))
        assertNull(routeForLink("somewhere-new"))
    }

    @Test
    fun timeAgoPicksTheCoarsestUnit() {
        val now = 10 * DAY
        assertEquals(AgoUnit.NOW to 0L, timeAgo(now - 30_000, now))
        assertEquals(AgoUnit.MINUTES to 5L, timeAgo(now - 5 * MINUTE, now))
        assertEquals(AgoUnit.HOURS to 3L, timeAgo(now - 3 * HOUR - MINUTE, now))
        assertEquals(AgoUnit.DAYS to 2L, timeAgo(now - 2 * DAY, now))
        assertEquals(AgoUnit.NOW to 0L, timeAgo(now + MINUTE, now)) // clock skew never goes negative
    }

    @Test
    fun groupsTodayApartFromEarlier() {
        val now = 10 * DAY + 12 * HOUR
        val items = listOf(item("today", now - HOUR), item("yesterday", now - DAY))
        val state = NotificationsState(items = items, now = now, isLoading = false)
        val (today, earlier) = state.grouped(TimeZone.UTC)
        assertEquals(listOf("today"), today.map { it.id })
        assertEquals(listOf("yesterday"), earlier.map { it.id })
        assertEquals(2, state.unreadCount)
        assertEquals(emptyList(), NotificationsState(items = items).grouped().first) // no clock yet
    }

    private fun item(id: String, at: Long) = AppNotification(id, NotificationKind.SPIN_READY, "t", "b", NotificationLinks.SPIN, at, read = false)

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
