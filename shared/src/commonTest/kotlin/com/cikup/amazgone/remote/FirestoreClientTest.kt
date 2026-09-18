package com.cikup.amazgone.remote

import com.cikup.amazgone.core.remote.FieldTransform
import com.cikup.amazgone.core.remote.FirestoreException
import com.cikup.amazgone.core.remote.FirestoreValues
import com.cikup.amazgone.core.remote.FirestoreWrite
import com.cikup.amazgone.core.remote.Precondition
import com.cikup.amazgone.core.remote.TransactionResult
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreClientTest {
    private val backend = FakeFirebase()

    @Test
    fun valuesRoundTrip() {
        val fields = mapOf("s" to "x", "n" to 5L, "b" to true, "d" to 1.5, "l" to listOf("a", 2L), "m" to mapOf("k" to null))
        val decoded = FirestoreValues.decodeFields(FirestoreValues.encodeFields(fields))
        assertEquals(fields, decoded)
    }

    @Test
    fun getDecodesDocumentsAndMapsMissingToNull() = runTest {
        backend.onPathEnds("GET", "users/u1", ok(userDoc("u1", coins = 700)))
        val firestore = backend.firestore()

        val doc = firestore.get("users/u1")!!
        assertEquals("u1", doc.id)
        assertEquals(700L, doc.fields["coins"])
        assertTrue((doc.fields["createdAt"] as Long) > 0)
        assertEquals("Bearer token-1", backend.requests.single().headers[HttpHeaders.Authorization])
        assertEquals("com.cikup.amazgone", backend.requests.single().headers["X-Ios-Bundle-Identifier"])
        assertNull(firestore.get("users/missing"))
    }

    @Test
    fun commitEncodesPreconditionsMasksAndTransforms() = runTest {
        backend.onPathEnds("POST", ":commit", ok("{}"))
        backend.firestore().commit(
            listOf(
                FirestoreWrite.Set("users/u1", mapOf("coins" to 10L), mask = listOf("coins"), precondition = Precondition.Exists(false)),
                FirestoreWrite.Transform("users/u1", listOf(FieldTransform.Increment("xp", 5), FieldTransform.ServerTimestamp("at"))),
            ),
        )
        val writes = backend.bodies.single()!!["writes"]!!.jsonArray
        val set = writes[0].jsonObject
        assertEquals("$DOCS/users/u1", set["update"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("false", set["currentDocument"]!!.jsonObject["exists"]!!.jsonPrimitive.content)
        assertEquals("coins", set["updateMask"]!!.jsonObject["fieldPaths"]!!.jsonArray.single().jsonPrimitive.content)
        val transforms = writes[1].jsonObject["transform"]!!.jsonObject["fieldTransforms"]!!.jsonArray
        assertEquals("5", transforms[0].jsonObject["increment"]!!.jsonObject["integerValue"]!!.jsonPrimitive.content)
        assertEquals("REQUEST_TIME", transforms[1].jsonObject["setToServerValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun setCanCarryTransformsInTheSameWrite() = runTest {
        backend.onPathEnds("POST", ":commit", ok("{}"))
        backend.firestore().commit(
            listOf(FirestoreWrite.Set("users/u1", mapOf("coins" to 1L), mask = listOf("coins"), transforms = listOf(FieldTransform.ServerTimestamp("lastSpinAt")))),
        )
        val write = backend.bodies.single()!!["writes"]!!.jsonArray.single().jsonObject
        assertEquals(
            "lastSpinAt",
            write["updateTransforms"]!!.jsonArray.single().jsonObject["fieldPath"]!!.jsonPrimitive.content,
            "rules must see the coin change and the timestamp in one write",
        )
    }

    @Test
    fun errorsAreMappedToTypedExceptions() = runTest {
        backend.onPathEnds("POST", ":commit", error(HttpStatusCode.BadRequest, "FAILED_PRECONDITION"))
        assertFailsWith<FirestoreException.PreconditionFailed> { backend.firestore().commit(listOf(FirestoreWrite.Delete("a/b"))) }
    }

    @Test
    fun permissionDeniedIsDistinctFromOutages() = runTest {
        backend.onPathEnds("GET", "users/u1", error(HttpStatusCode.Forbidden, "PERMISSION_DENIED"))
        assertFailsWith<FirestoreException.PermissionDenied> { backend.firestore().get("users/u1") }
        backend.onPathEnds("GET", "users/u2", Reply(HttpStatusCode.ServiceUnavailable, "{}"))
        assertFailsWith<FirestoreException.Unavailable> { backend.firestore().get("users/u2") }
    }

    @Test
    fun expiredTokenIsRefreshedOnceAndRetried() = runTest {
        backend.onPathEnds("GET", "users/u1", error(HttpStatusCode.Unauthorized, "UNAUTHENTICATED"), ok(userDoc("u1", 1)))
        val tokens = StaticTokens("stale", "fresh")

        backend.firestore(tokens).get("users/u1")

        assertEquals(1, tokens.refreshes)
        assertEquals("Bearer fresh", backend.requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun transactionsAreOptimisticPinReadsAndRetryOnConflict() = runTest {
        backend.onPathEnds("GET", "users/u1", ok(userDoc("u1", coins = 10)))
        backend.onPathEnds("POST", ":commit", error(HttpStatusCode.BadRequest, "FAILED_PRECONDITION"), ok("{}"))
        var attempts = 0

        val result = backend.firestore().runTransaction { tx ->
            attempts++
            tx.get("users/u1")
            tx.get("users/u1/orders/o1") // missing
            TransactionResult(
                listOf(FirestoreWrite.Set("users/u1", mapOf("coins" to 5L)), FirestoreWrite.Set("users/u1/orders/o1", mapOf("n" to 1L))),
                "done",
            )
        }

        assertEquals("done", result)
        assertEquals(2, attempts, "conflict re-runs the block")
        assertTrue(backend.requests.none { it.url.encodedPath.endsWith(":beginTransaction") }, "end-user tokens may not use server transactions")
        val writes = backend.bodies.last()!!["writes"]!!.jsonArray
        assertEquals("2026-01-01T00:00:00Z", writes[0].jsonObject["currentDocument"]!!.jsonObject["updateTime"]!!.jsonPrimitive.content)
        assertEquals("false", writes[1].jsonObject["currentDocument"]!!.jsonObject["exists"]!!.jsonPrimitive.content)
    }

    @Test
    fun queryReturnsDocumentsInOrder() = runTest {
        backend.onPathEnds(
            "POST", ":runQuery",
            ok("""[{"document":${userDoc("a", 1, xp = 900)}},{"document":${userDoc("b", 1, xp = 50)}},{"readTime":"x"}]"""),
        )
        val docs = backend.firestore().query("leaderboard", orderBy = "xp", descending = true, limit = 2)
        assertEquals(listOf("a", "b"), docs.map { it.id })
    }
}
