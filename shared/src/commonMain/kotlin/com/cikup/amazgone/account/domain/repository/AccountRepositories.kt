package com.cikup.amazgone.account.domain.repository

import com.cikup.amazgone.account.domain.model.UserProfile
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.core.domain.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<UserSession?>
    /** Set when the server revoked the login (refresh token rejected); sync pauses until the user signs in again. */
    val expiredSession: StateFlow<UserSession?>
    val isRemoteAvailable: Boolean
    suspend fun register(username: String, password: String): DomainResult<UserSession>
    suspend fun login(username: String, password: String): DomainResult<UserSession>
    suspend fun logout()
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    /** Creates the server profile (with the starter coins) right after registration. */
    suspend fun createRemoteProfile(session: UserSession): DomainResult<Unit>
}
