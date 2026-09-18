package com.cikup.amazgone.account

import com.cikup.amazgone.account.data.remote.AuthRepositoryImpl
import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.core.remote.FirebaseAuthApi
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.storage.InMemorySecureStore
import com.cikup.amazgone.remote.FakeFirebase
import com.cikup.amazgone.remote.TEST_CONFIG
import com.cikup.amazgone.remote.error
import com.cikup.amazgone.remote.ok
import com.cikup.amazgone.testing.FakeClock
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

private const val TOKENS = """{"localId":"uid-1","idToken":"id-1","refreshToken":"refresh-1","expiresIn":"3600"}"""

class AuthRepositoryTest {
    private val backend = FakeFirebase()
    private val store = InMemorySecureStore()
    private val clock = FakeClock()

    private fun repo(api: FirebaseAuthApi? = FirebaseAuthApi(backend.client, TEST_CONFIG)) =
        AuthRepositoryImpl(api, store, clock, PrintLogger)

    @Test
    fun registerUsesSyntheticEmailAndPersistsTheSession() = runTest {
        backend.onPathEnds("POST", "accounts:signUp", ok(TOKENS))
        val repo = repo()

        val result = repo.register("bob", "password1")

        assertIs<DomainResult.Success<*>>(result)
        assertEquals("bob@amazgone.local", backend.bodies.single()!!["email"]!!.jsonPrimitive.content)
        assertEquals("true", backend.bodies.single()!!["returnSecureToken"]!!.jsonPrimitive.content, "without it Firebase omits the refresh token")
        assertEquals("uid-1", repo.session.value?.uid)
        assertEquals("refresh-1", store.get("auth.refresh_token"))
        assertEquals("uid-1", repo().session.value?.uid, "session survives an app restart")
    }

    @Test
    fun takenUsernameAndBadPasswordMapToFieldErrors() = runTest {
        backend.onPathEnds("POST", "accounts:signUp", error(HttpStatusCode.BadRequest, "EMAIL_EXISTS"))
        backend.onPathEnds("POST", "accounts:signInWithPassword", error(HttpStatusCode.BadRequest, "INVALID_LOGIN_CREDENTIALS"))
        val repo = repo()

        val taken = (repo.register("bob", "password1") as DomainResult.Failure).error as DomainError.Validation
        assertEquals(ValidationReason.TAKEN, taken.reason)
        val wrong = (repo.login("bob", "nope-nope") as DomainResult.Failure).error as DomainError.Validation
        assertEquals(ValidationReason.INVALID_CREDENTIALS, wrong.reason)
        assertNull(repo.session.value)
    }

    @Test
    fun disabledProviderReportsRemoteUnavailable() = runTest {
        backend.onPathEnds("POST", "accounts:signInWithPassword", error(HttpStatusCode.BadRequest, "CONFIGURATION_NOT_FOUND"))
        assertEquals(DomainError.RemoteUnavailable, (repo().login("bob", "password1") as DomainResult.Failure).error)
        assertEquals(DomainError.RemoteUnavailable, (repo(api = null).login("bob", "password1") as DomainResult.Failure).error)
    }

    @Test
    fun idTokenIsCachedThenRefreshedNearExpiry() = runTest {
        backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        backend.onPathEnds("POST", "/token", ok("""{"id_token":"id-2","refresh_token":"refresh-2","expires_in":"3600","user_id":"uid-1"}"""))
        val repo = repo()
        repo.login("bob", "password1")

        assertEquals("id-1", repo.idToken())
        clock.advance(3_600_000)
        assertEquals("id-2", repo.idToken())
        assertEquals("refresh-2", store.get("auth.refresh_token"))
    }

    @Test
    fun revokedRefreshTokenExpiresTheSessionWithoutLosingTheUser() = runTest {
        backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        backend.onPathEnds("POST", "/token", error(HttpStatusCode.BadRequest, "TOKEN_EXPIRED"))
        val repo = repo()
        repo.login("bob", "password1")
        clock.advance(3_600_000)

        assertFailsWith<FirestoreException.Unauthenticated> { repo.idToken() }

        assertNull(repo.session.value, "sync gate closes")
        assertEquals("bob", repo.expiredSession.value?.username)
        assertEquals("bob", repo().expiredSession.value?.username, "survives restart")
        repo.login("bob", "password1")
        assertNull(repo.expiredSession.value)
    }

    @Test
    fun logoutForgetsEverything() = runTest {
        backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        val repo = repo()
        repo.login("bob", "password1")

        repo.logout()

        assertNull(repo.session.value)
        assertNull(store.get("auth.refresh_token"))
        assertFailsWith<FirestoreException.Unauthenticated> { repo.idToken() }
    }
}
