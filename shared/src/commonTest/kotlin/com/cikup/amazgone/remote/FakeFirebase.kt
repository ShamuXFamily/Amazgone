package com.cikup.amazgone.remote

import com.cikup.amazgone.core.network.AppJson
import com.cikup.amazgone.core.network.createHttpClient
import com.cikup.amazgone.core.remote.FirebaseConfig
import com.cikup.amazgone.core.remote.FirestoreClient
import com.cikup.amazgone.core.remote.IdTokenProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

val TEST_CONFIG = FirebaseConfig(apiKey = "key", projectId = "demo", iosBundleId = "com.cikup.amazgone")
const val DOCS = "projects/demo/databases/(default)/documents"

data class Reply(val status: HttpStatusCode, val body: String)

fun ok(body: String) = Reply(HttpStatusCode.OK, body)
fun error(status: HttpStatusCode, grpc: String) = Reply(status, """{"error":{"code":${status.value},"message":"$grpc","status":"$grpc"}}""")

/** Scriptable Firebase REST backend: route by "METHOD path-suffix" → queue of replies; the most recent matching stub wins. */
class FakeFirebase {
    val requests = mutableListOf<HttpRequestData>()
    val bodies = mutableListOf<JsonObject?>()
    private val routes = mutableListOf<Pair<(HttpRequestData) -> Boolean, ArrayDeque<Reply>>>()

    fun on(matcher: (HttpRequestData) -> Boolean, vararg replies: Reply) {
        routes += matcher to ArrayDeque(replies.toList())
    }

    fun onPathEnds(method: String, suffix: String, vararg replies: Reply) =
        on({ it.method.value == method && it.url.encodedPath.endsWith(suffix) }, *replies)

    val engine = MockEngine { request ->
        requests += request
        bodies += (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()
            ?.let { runCatching { AppJson.parseToJsonElement(it).jsonObject }.getOrNull() }
        val queue = routes.lastOrNull { it.first(request) }?.second
        val reply = when {
            queue == null -> Reply(HttpStatusCode.NotFound, """{"error":{"status":"NOT_FOUND"}}""")
            queue.size > 1 -> queue.removeFirst()
            else -> queue.first()
        }
        respond(reply.body, reply.status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    val client = createHttpClient(engine)

    fun firestore(tokens: IdTokenProvider = StaticTokens()) = FirestoreClient(client, TEST_CONFIG, tokens)
}

class StaticTokens(private vararg val tokens: String = arrayOf("token-1")) : IdTokenProvider {
    var refreshes = 0
    override suspend fun idToken(forceRefresh: Boolean): String {
        if (forceRefresh) refreshes++
        return tokens[refreshes.coerceAtMost(tokens.size - 1)]
    }
}

fun userDoc(uid: String, coins: Long, xp: Long = 0, username: String = "bob") = """
{"name":"$DOCS/users/$uid","updateTime":"2026-01-01T00:00:00Z","fields":{
"username":{"stringValue":"$username"},"coins":{"integerValue":"$coins"},"xp":{"integerValue":"$xp"},
"level":{"integerValue":"1"},"orderCount":{"integerValue":"0"},"createdAt":{"timestampValue":"2026-01-01T00:00:00Z"}}}
""".trimIndent()
