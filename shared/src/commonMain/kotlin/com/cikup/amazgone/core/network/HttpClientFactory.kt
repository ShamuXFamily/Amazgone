package com.cikup.amazgone.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Lenient JSON shared by network, seed import and the outbox payloads. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

object NetworkConfig {
    /** CheapShark rejects generic user agents; identify the app descriptively. */
    const val USER_AGENT = "Amazgone/1.0 (Kotlin Multiplatform demo shop)"
    const val REQUEST_TIMEOUT_MS = 20_000L
    const val CONNECT_TIMEOUT_MS = 10_000L
}

fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(AppJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = NetworkConfig.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = NetworkConfig.CONNECT_TIMEOUT_MS
    }
    defaultRequest { headers.append(HttpHeaders.UserAgent, NetworkConfig.USER_AGENT) }
}
