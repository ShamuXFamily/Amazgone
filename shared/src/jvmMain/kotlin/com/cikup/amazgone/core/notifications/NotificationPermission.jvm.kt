package com.cikup.amazgone.core.notifications

import androidx.compose.runtime.Composable

private object Granted : NotificationPermission {
    override val granted = true
    override fun request() = Unit
}

@Composable
actual fun rememberNotificationPermission(): NotificationPermission = Granted
