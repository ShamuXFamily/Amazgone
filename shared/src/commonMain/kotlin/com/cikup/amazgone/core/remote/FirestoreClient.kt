package com.cikup.amazgone.core.remote

import com.cikup.amazgone.core.network.AppJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Supplies a valid Firebase ID token for the signed-in user (refreshing when needed). */
interface IdTokenProvider {
    /** @throws FirestoreException.Unauthenticated when nobody is signed in. */
    suspend fun idToken(forceRefresh: Boolean = false): String
}

/**
 * Minimal Cloud Firestore REST client: documents, atomic commits (with preconditions and
 * server-side transforms), transactions and structured queries. Security rules apply as usual.
 */
class FirestoreClient(
    private val client: HttpClient,
    private val config: FirebaseConfig,
    private val tokens: IdTokenProvider,
    private val baseUrl: String = "https://firestore.googleapis.com/v1",
) {
    private val databasePath = "projects/${config.projectId}/databases/(default)"
    private val documentsPath = "$databasePath/documents"

    fun documentName(path: String) = "$documentsPath/${path.trimStart('/')}"

    suspend fun get(path: String): FirestoreDocument? = try {
        authorized { token ->
            client.get("$baseUrl/${documentName(path)}") {
                bearerAuth(token)
                appIdentity(config)
            }.body<JsonObject>().toDocument()
        }
    } catch (_: FirestoreException.NotFound) {
        null
    }

    /**
     * Lists a collection (all pages). [authenticated] = false reads a public collection (rules allow
     * `read: if true`) without a signed-in user, identified only by the API key.
     */
    suspend fun list(collectionPath: String, pageSize: Int = PAGE_SIZE, authenticated: Boolean = true): List<FirestoreDocument> {
        val documents = mutableListOf<FirestoreDocument>()
        var pageToken: String? = null
        do {
            val request: suspend (String?) -> JsonObject = { token ->
                client.get("$baseUrl/${documentName(collectionPath)}") {
                    if (token != null) bearerAuth(token) else parameter("key", config.apiKey)
                    appIdentity(config)
                    parameter("pageSize", pageSize)
                    pageToken?.let { parameter("pageToken", it) }
                }.body<JsonObject>()
            }
            val page = if (authenticated) authorized { request(it) } else mapErrors { request(null) }
            page["documents"]?.jsonArray?.mapTo(documents) { it.jsonObject.toDocument() }
            pageToken = (page["nextPageToken"] as? JsonPrimitive)?.content
        } while (pageToken != null)
        return documents
    }

    suspend fun commit(writes: List<FirestoreWrite>) {
        if (writes.isEmpty()) return
        val body = buildJsonObject { put("writes", JsonArray(writes.map { encodeWrite(it) })) }
        authorized { token ->
            client.post("$baseUrl/$documentsPath:commit") {
                bearerAuth(token)
                appIdentity(config)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<JsonObject>()
        }
    }

    /**
     * Read-modify-write with optimistic concurrency, the way the client SDKs do it: server-side
     * transactions (beginTransaction) are not permitted for end-user tokens. Documents read through
     * [TransactionScope.get] are pinned — writes to them commit only if they are unchanged
     * (updateTime precondition) or still absent. On a conflict the whole [block] runs again, so
     * blocks must re-check their own idempotency marker (e.g. "order doc already exists").
     */
    suspend fun <T> runTransaction(block: suspend (TransactionScope) -> TransactionResult<T>): T {
        var attempt = 0
        while (true) {
            val scope = TransactionScope(this)
            val result = block(scope)
            if (result.writes.isEmpty()) return result.value
            try {
                commit(scope.pin(result.writes))
                return result.value
            } catch (conflict: FirestoreException.PreconditionFailed) {
                if (++attempt >= MAX_TRANSACTION_ATTEMPTS) throw conflict
            } catch (aborted: FirestoreException.Aborted) {
                if (++attempt >= MAX_TRANSACTION_ATTEMPTS) throw aborted
            }
        }
    }

    /** Structured query over a top-level collection. */
    suspend fun query(collectionId: String, orderBy: String, descending: Boolean, limit: Int): List<FirestoreDocument> {
        val query = buildJsonObject {
            put("structuredQuery", buildJsonObject {
                put("from", buildJsonArray { add(buildJsonObject { put("collectionId", collectionId) }) })
                put("orderBy", buildJsonArray {
                    add(buildJsonObject {
                        put("field", buildJsonObject { put("fieldPath", orderBy) })
                        put("direction", if (descending) "DESCENDING" else "ASCENDING")
                    })
                })
                put("limit", limit)
            })
        }
        val rows = authorized { token ->
            client.post("$baseUrl/$documentsPath:runQuery") {
                bearerAuth(token)
                appIdentity(config)
                contentType(ContentType.Application.Json)
                setBody(query)
            }.body<JsonArray>()
        }
        return rows.mapNotNull { row -> row.jsonObject["document"]?.jsonObject?.toDocument() }
    }

    /** Adds the ID token; on 401 refreshes once and retries. Maps HTTP errors to [FirestoreException]. */
    private suspend fun <T> authorized(call: suspend (token: String) -> T): T = try {
        mapErrors { call(tokens.idToken()) }
    } catch (_: FirestoreException.Unauthenticated) {
        mapErrors { call(tokens.idToken(forceRefresh = true)) }
    }

    private suspend fun <T> mapErrors(call: suspend () -> T): T = try {
        call()
    } catch (e: ClientRequestException) {
        throw toFirestoreException(e.response.status, e.response.bodyAsText())
    } catch (e: ServerResponseException) {
        throw FirestoreException.Unavailable("HTTP ${e.response.status.value}")
    }

    private fun encodeWrite(write: FirestoreWrite): JsonObject = buildJsonObject {
        when (write) {
            is FirestoreWrite.Set -> {
                put("update", buildJsonObject {
                    put("name", documentName(write.path))
                    put("fields", FirestoreValues.encodeFields(write.fields))
                })
                write.mask?.let { mask ->
                    put("updateMask", buildJsonObject { put("fieldPaths", JsonArray(mask.map { JsonPrimitive(it) })) })
                }
                write.precondition?.let { put("currentDocument", encodePrecondition(it)) }
                if (write.transforms.isNotEmpty()) put("updateTransforms", JsonArray(write.transforms.map { encodeTransform(it) }))
            }
            is FirestoreWrite.Delete -> {
                put("delete", documentName(write.path))
                write.precondition?.let { put("currentDocument", encodePrecondition(it)) }
            }
            is FirestoreWrite.Transform -> put("transform", buildJsonObject {
                put("document", documentName(write.path))
                put("fieldTransforms", JsonArray(write.transforms.map { encodeTransform(it) }))
            })
        }
    }

    private fun encodePrecondition(p: Precondition): JsonObject = buildJsonObject {
        when (p) {
            is Precondition.Exists -> put("exists", p.exists)
            is Precondition.UpdateTime -> put("updateTime", p.updateTime)
        }
    }

    private fun encodeTransform(t: FieldTransform): JsonElement = buildJsonObject {
        put("fieldPath", t.field)
        when (t) {
            is FieldTransform.Increment -> put("increment", FirestoreValues.encode(t.by))
            is FieldTransform.ServerTimestamp -> put("setToServerValue", "REQUEST_TIME")
        }
    }

    private fun JsonObject.toDocument() = FirestoreDocument(
        path = this["name"]!!.jsonPrimitive.content.substringAfter("$documentsPath/"),
        fields = FirestoreValues.decodeFields(this["fields"]?.jsonObject),
        updateTime = (this["updateTime"] as? JsonPrimitive)?.content,
    )

    internal companion object {
        const val PAGE_SIZE = 100
        const val MAX_TRANSACTION_ATTEMPTS = 5

        fun toFirestoreException(status: HttpStatusCode, body: String): FirestoreException {
            val error = runCatching { AppJson.parseToJsonElement(body).jsonObject["error"]?.jsonObject }.getOrNull()
            val grpc = (error?.get("status") as? JsonPrimitive)?.content
            val message = (error?.get("message") as? JsonPrimitive)?.content ?: "HTTP ${status.value}"
            return when {
                grpc == "FAILED_PRECONDITION" || grpc == "ALREADY_EXISTS" -> FirestoreException.PreconditionFailed(message)
                grpc == "ABORTED" || status == HttpStatusCode.Conflict -> FirestoreException.Aborted(message)
                grpc == "PERMISSION_DENIED" || status == HttpStatusCode.Forbidden -> FirestoreException.PermissionDenied(message)
                grpc == "NOT_FOUND" || status == HttpStatusCode.NotFound -> FirestoreException.NotFound(message)
                grpc == "UNAUTHENTICATED" || status == HttpStatusCode.Unauthorized -> FirestoreException.Unauthenticated(message)
                else -> FirestoreException.Unavailable(message)
            }
        }
    }
}

/** Writes to commit plus the value the transaction returns. */
data class TransactionResult<T>(val writes: List<FirestoreWrite>, val value: T)

