package com.cikup.amazgone.core.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val KEY_LINK = "link"
private const val MIN_DELAY_SECONDS = 1.0

/** iOS: UNUserNotificationCenter schedules each notification for its exact time (fires with the app closed). */
class IosSystemNotifications : SystemNotifications {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(id: String, title: String, body: String, atMillis: Long, link: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf(KEY_LINK to link))
        }
        val seconds = (atMillis / 1_000.0 - NSDate().timeIntervalSince1970).coerceAtLeast(MIN_DELAY_SECONDS)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), withCompletionHandler = null)
    }

    override fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
    }
}

/**
 * Routes taps on notifications to [DeepLinks]. While the app is open, the in-app banner shows the
 * news instead, so system banners are suppressed in the foreground.
 */
class NotificationTapHandler : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        (didReceiveNotificationResponse.notification.request.content.userInfo[KEY_LINK] as? String)?.let(DeepLinks::open)
        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(0uL)
    }
}

private val tapHandler = NotificationTapHandler()

/** Call once at launch, before the first notification can be tapped. */
fun installNotificationTapHandler() {
    UNUserNotificationCenter.currentNotificationCenter().delegate = tapHandler
}

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    // assume granted until the settings answer, so the "turn on" card never flashes
    var granted by remember { mutableStateOf(true) }
    val center = UNUserNotificationCenter.currentNotificationCenter()
    LaunchedEffect(Unit) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val ok = settings?.authorizationStatus == UNAuthorizationStatusAuthorized || settings?.authorizationStatus == UNAuthorizationStatusProvisional
            dispatch_async(dispatch_get_main_queue()) { granted = ok }
        }
    }
    return remember(granted) {
        object : NotificationPermission {
            override val granted = granted
            override fun request() {
                center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge) { ok, _ ->
                    dispatch_async(dispatch_get_main_queue()) { granted = ok }
                }
            }
        }
    }
}
