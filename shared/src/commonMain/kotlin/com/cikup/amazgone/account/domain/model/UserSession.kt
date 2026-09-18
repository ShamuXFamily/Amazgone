package com.cikup.amazgone.account.domain.model

/** A signed-in account. Guests have no session and keep everything on the device. */
data class UserSession(val uid: String, val username: String)

/** Server-side profile snapshot cached locally (wallet balances come from the ledger). */
data class UserProfile(
    val uid: String,
    val username: String,
    val orderCount: Int,
    val lastSpinAt: Long?,
    val lastScratchAt: Long?,
    val createdAt: Long,
    val syncedAt: Long,
)
