package com.cikup.amazgone.core.domain

/** Implemented by every store holding per-user data; all are wiped on sign-out. */
interface UserScopedStore {
    suspend fun clearUserData()
}
