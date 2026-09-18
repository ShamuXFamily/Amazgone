package com.cikup.amazgone.account.data.remote

import com.cikup.amazgone.account.domain.model.CredentialsValidator
import com.cikup.amazgone.account.domain.model.UserSession
import com.cikup.amazgone.account.domain.repository.AuthRepository
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.core.remote.AuthApiException
import com.cikup.amazgone.core.remote.AuthTokens
import com.cikup.amazgone.core.remote.FirebaseAuthApi
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.IdTokenProvider
import com.cikup.amazgone.core.storage.SecureStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Username/password auth on Firebase (REST). The refresh token lives in [SecureStore], so a
 * signed-in user stays signed in offline; ID tokens are short-lived and kept in memory only.
 */
class AuthRepositoryImpl(
    private val api: FirebaseAuthApi?,
    private val secureStore: SecureStore,
    private val time: TimeProvider,
    private val logger: AppLogger,
) : AuthRepository, IdTokenProvider {

    private val mutableSession = MutableStateFlow(restoreSession())
    override val session: StateFlow<UserSession?> = mutableSession.asStateFlow()

    private val mutableExpired = MutableStateFlow(restoreExpired())
    override val expiredSession: StateFlow<UserSession?> = mutableExpired.asStateFlow()
    override val isRemoteAvailable: Boolean get() = api != null

    private val tokenMutex = Mutex()
    private var idToken: String? = null
    private var idTokenExpiresAt: Long = 0

    override suspend fun register(username: String, password: String) =
        authenticate(username) { it.signUp(CredentialsValidator.syntheticEmail(username), password) }

    override suspend fun login(username: String, password: String) =
        authenticate(username) { it.signIn(CredentialsValidator.syntheticEmail(username), password) }

    override suspend fun logout() {
        tokenMutex.withLock {
            idToken = null
            idTokenExpiresAt = 0
        }
        (KEYS + EXPIRED_KEYS).forEach(secureStore::remove)
        mutableSession.value = null
        mutableExpired.value = null
    }

    override suspend fun idToken(forceRefresh: Boolean): String = tokenMutex.withLock {
        val cached = idToken
        if (!forceRefresh && cached != null && time.nowMillis() < idTokenExpiresAt - REFRESH_MARGIN_MS) return cached
        val refreshToken = secureStore.get(KEY_REFRESH) ?: throw FirestoreException.Unauthenticated("Not signed in")
        val authApi = api ?: throw FirestoreException.Unavailable("Firebase is not configured")
        val tokens = try {
            authApi.refresh(refreshToken)
        } catch (e: AuthApiException.InvalidCredentials) {
            logger.error(TAG, "Refresh token rejected; session expired", e)
            expireSession()
            throw FirestoreException.Unauthenticated(e.code)
        }
        store(tokens, mutableSession.value?.username.orEmpty())
        tokens.idToken
    }

    private suspend fun authenticate(
        username: String,
        call: suspend (FirebaseAuthApi) -> AuthTokens,
    ): DomainResult<UserSession> {
        val authApi = api ?: return DomainResult.Failure(DomainError.RemoteUnavailable)
        return try {
            val tokens = call(authApi)
            tokenMutex.withLock { store(tokens, username) }
            val session = UserSession(tokens.uid, username)
            EXPIRED_KEYS.forEach(secureStore::remove)
            mutableExpired.value = null
            mutableSession.value = session
            DomainResult.Success(session)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: AuthApiException) {
            logger.error(TAG, "Auth failed: ${e.code}", e)
            DomainResult.Failure(e.toDomainError())
        } catch (t: Throwable) {
            logger.error(TAG, "Auth request failed", t)
            DomainResult.Failure(DomainError.Network)
        }
    }

    /** Caller holds [tokenMutex]. */
    private fun store(tokens: AuthTokens, username: String) {
        idToken = tokens.idToken
        idTokenExpiresAt = time.nowMillis() + (tokens.expiresIn.toLongOrNull() ?: DEFAULT_TTL_S) * MILLIS
        secureStore.put(KEY_REFRESH, tokens.refreshToken)
        secureStore.put(KEY_UID, tokens.uid)
        if (username.isNotBlank()) secureStore.put(KEY_USERNAME, username)
    }

    /**
     * Keeps local data and the outbox (so nothing is refunded) but closes the sync gate by dropping
     * the session. Caller holds [tokenMutex].
     */
    private fun expireSession() {
        val current = mutableSession.value ?: return
        idToken = null
        idTokenExpiresAt = 0
        secureStore.remove(KEY_REFRESH)
        secureStore.put(KEY_EXPIRED_UID, current.uid)
        secureStore.put(KEY_EXPIRED_USERNAME, current.username)
        mutableExpired.value = current
        mutableSession.value = null
    }

    private fun restoreExpired(): UserSession? {
        val uid = secureStore.get(KEY_EXPIRED_UID) ?: return null
        return UserSession(uid, secureStore.get(KEY_EXPIRED_USERNAME).orEmpty())
    }

    private fun restoreSession(): UserSession? {
        val uid = secureStore.get(KEY_UID) ?: return null
        val username = secureStore.get(KEY_USERNAME) ?: return null
        return if (secureStore.get(KEY_REFRESH) != null) UserSession(uid, username) else null
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val KEY_REFRESH = "auth.refresh_token"
        const val KEY_UID = "auth.uid"
        const val KEY_USERNAME = "auth.username"
        const val KEY_EXPIRED_UID = "auth.expired_uid"
        const val KEY_EXPIRED_USERNAME = "auth.expired_username"
        val KEYS = listOf(KEY_REFRESH, KEY_UID, KEY_USERNAME)
        val EXPIRED_KEYS = listOf(KEY_EXPIRED_UID, KEY_EXPIRED_USERNAME)
        const val REFRESH_MARGIN_MS = 5 * 60 * 1_000L
        const val DEFAULT_TTL_S = 3_600L
        const val MILLIS = 1_000L
    }
}

private fun AuthApiException.toDomainError(): DomainError = when (this) {
    is AuthApiException.EmailExists -> DomainError.Validation(CredentialsValidator.FIELD_USERNAME, ValidationReason.TAKEN)
    is AuthApiException.InvalidCredentials ->
        DomainError.Validation(CredentialsValidator.FIELD_PASSWORD, ValidationReason.INVALID_CREDENTIALS)
    is AuthApiException.WeakPassword -> DomainError.Validation(CredentialsValidator.FIELD_PASSWORD, ValidationReason.TOO_SHORT)
    is AuthApiException.TooManyAttempts -> DomainError.TooManyAttempts
    is AuthApiException.NotConfigured -> DomainError.RemoteUnavailable
    is AuthApiException.Other -> DomainError.Unknown(code)
}
