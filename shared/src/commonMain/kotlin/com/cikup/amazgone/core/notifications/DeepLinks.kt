package com.cikup.amazgone.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Links from tapped system notifications (iOS delegate / Android launch intent) waiting for the app
 * shell to navigate. A process-wide singleton because the OS hands them to platform code, not Compose.
 */
object DeepLinks {
    private val mutablePending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = mutablePending.asStateFlow()

    fun open(link: String) {
        mutablePending.value = link
    }

    fun consume() {
        mutablePending.value = null
    }
}
