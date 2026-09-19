package com.cikup.amazgone.navigation

import androidx.navigation.NavHostController
import com.cikup.amazgone.notifications.domain.model.NotificationLinks

private const val ORDER_PREFIX = "order/"

/** Maps a notification link ("order/{id}", "spin"…) to its screen; null for links this build doesn't know. */
internal fun routeForLink(link: String): Route? = when {
    link.startsWith(ORDER_PREFIX) -> link.removePrefix(ORDER_PREFIX).takeIf { it.isNotBlank() }?.let { Route.OrderDetail(it) }
    link == NotificationLinks.SPIN -> Route.SpinWheel
    link == NotificationLinks.SCRATCH -> Route.ScratchCard
    link == NotificationLinks.FLASH_SALE -> Route.FlashSale
    link == NotificationLinks.GARAGE -> Route.Garage
    link == NotificationLinks.ACHIEVEMENTS -> Route.Achievements
    else -> null
}

/** Opens [link]'s screen on top of the current stack (single-top, so repeats don't pile up). */
internal fun NavHostController.openLink(link: String) {
    val route = routeForLink(link) ?: return
    navigate(route) { launchSingleTop = true }
}
