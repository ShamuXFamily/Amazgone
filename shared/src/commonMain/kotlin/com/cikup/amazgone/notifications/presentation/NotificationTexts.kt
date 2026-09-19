package com.cikup.amazgone.notifications.presentation

import amazgone.shared.generated.resources.*
import com.cikup.amazgone.delivery.domain.model.Courier
import com.cikup.amazgone.delivery.presentation.courierNameRes
import com.cikup.amazgone.notifications.domain.model.NotificationKind
import com.cikup.amazgone.notifications.domain.model.PlannedNotification
import com.cikup.amazgone.notifications.domain.repository.NotificationRenderer
import com.cikup.amazgone.progress.domain.model.AchievementId
import com.cikup.amazgone.progress.presentation.label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Localized notification text from string resources (works outside composition). */
class ResourceNotificationRenderer : NotificationRenderer {
    override suspend fun render(notification: PlannedNotification): Pair<String, String> {
        val a = notification.args
        return when (notification.kind) {
            NotificationKind.ORDER_CONFIRMED -> text(Res.string.notif_ORDER_CONFIRMED_title) to text(Res.string.notif_ORDER_CONFIRMED_body, a[0])
            NotificationKind.ORDER_REJECTED -> text(Res.string.notif_ORDER_REJECTED_title) to text(Res.string.notif_ORDER_REJECTED_body, a[0])
            NotificationKind.COURIER_DEPARTED -> {
                val courier = courier(a[0])
                text(Res.string.notif_COURIER_DEPARTED_title, courierEmoji(a[0]) + " " + courier) to
                    text(Res.string.notif_COURIER_DEPARTED_body, courier.lowercase(), a[1], a[2])
            }
            NotificationKind.PARCEL_DELIVERED -> text(Res.string.notif_PARCEL_DELIVERED_title) to
                text(Res.string.notif_PARCEL_DELIVERED_body, a[1], courier(a[0]).lowercase())
            NotificationKind.REVIEW_PROMPT -> text(Res.string.notif_REVIEW_PROMPT_title) to text(Res.string.notif_REVIEW_PROMPT_body, a[0])
            NotificationKind.SPIN_READY -> text(Res.string.notif_SPIN_READY_title) to text(Res.string.notif_SPIN_READY_body)
            NotificationKind.SCRATCH_READY -> text(Res.string.notif_SCRATCH_READY_title) to text(Res.string.notif_SCRATCH_READY_body)
            NotificationKind.FLASH_SALE -> text(Res.string.notif_FLASH_SALE_title) to text(Res.string.notif_FLASH_SALE_body)
            NotificationKind.LEVEL_UP -> text(Res.string.notif_LEVEL_UP_title, a[0]) to text(Res.string.notif_LEVEL_UP_body)
            NotificationKind.ACHIEVEMENT -> {
                val name = AchievementId.entries.firstOrNull { it.name == a[0] }?.let { getString(it.label().title) } ?: a[0]
                text(Res.string.notif_ACHIEVEMENT_title) to text(Res.string.notif_ACHIEVEMENT_body, name)
            }
            NotificationKind.COURIER_UNLOCKED -> text(Res.string.notif_COURIER_UNLOCKED_title) to
                text(Res.string.notif_COURIER_UNLOCKED_body, courierEmoji(a[0]), courier(a[0]))
        }
    }

    private suspend fun text(res: StringResource, vararg args: Any): String = getString(res, *args)

    private suspend fun courier(name: String): String = Courier.parse(name)?.let { getString(courierNameRes(it)) } ?: name

    private fun courierEmoji(name: String): String = Courier.parse(name)?.emoji.orEmpty()
}
