package com.cikup.amazgone.core.notifications

import androidx.compose.runtime.Composable

/** Whether the OS lets the app show notifications, and a way to ask (iOS prompt / Android 13+ permission). */
interface NotificationPermission {
    val granted: Boolean
    fun request()
}

@Composable
expect fun rememberNotificationPermission(): NotificationPermission
