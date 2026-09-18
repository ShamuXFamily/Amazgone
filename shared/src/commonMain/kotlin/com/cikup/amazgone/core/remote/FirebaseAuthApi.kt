package com.cikup.amazgone.core.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AuthTokens(
    @SerialName("localId") val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
)

/** returnSecureToken must be sent explicitly: kotlinx.serialization omits default values, and without it Firebase returns no refresh token. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class PasswordRequest(
    val email: String,
    val password: String,
    @EncodeDefault val returnSecureToken: Boolean = true,
)

@Serializable
private data class RefreshResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("user_id") val uid: String,
)

/** Firebase Auth error codes mapped to meaningful cases. */
sealed class AuthApiException(val code: String) : Exception(code) {
    class EmailExists : AuthApiException("EMAIL_EXISTS")
    class InvalidCredentials(code: String) : AuthApiException(code)
    class WeakPassword : AuthApiException("WEAK_PASSWORD")
    class TooManyAttempts : AuthApiException("TOO_MANY_ATTEMPTS_TRY_LATER")
    /** Email/Password sign-in is not enabled in the Firebase console. */
    class NotConfigured(code: String) : AuthApiException(code)
    class Other(code: String) : AuthApiException(code)
}

/** Firebase Authentication over REST (Identity Toolkit + Secure Token APIs). */
class FirebaseAuthApi(
    private val client: HttpClient,
    private val config: FirebaseConfig,
    private val identityUrl: String = "https://identitytoolkit.googleapis.com/v1",
    private val tokenUrl: String = "https://securetoken.googleapis.com/v1/token",
) {
    suspend fun signUp(email: String, password: String): AuthTokens = call("accounts:signUp", email, password)

    suspend fun signIn(email: String, password: String): AuthTokens = call("accounts:signInWithPassword", email, password)

    suspend fun refresh(refreshToken: String): AuthTokens = mapErrors {
        val response = client.submitForm(
            url = tokenUrl,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            },
        ) {
            parameter("key", config.apiKey)
            appIdentity(config)
        }.body<RefreshResponse>()
        AuthTokens(response.uid, response.idToken, response.refreshToken, response.expiresIn)
    }

    private suspend fun call(endpoint: String, email: String, password: String): AuthTokens = mapErrors {
        client.post("$identityUrl/$endpoint") {
            parameter("key", config.apiKey)
            appIdentity(config)
            contentType(ContentType.Application.Json)
            setBody(PasswordRequest(email, password))
        }.body()
    }

    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: ClientRequestException) {
        throw parseError(e.response.bodyAsText())
    }

    internal companion object {
        fun parseError(body: String): AuthApiException {
            val message = runCatching {
                com.cikup.amazgone.core.network.AppJson.parseToJsonElement(body)
                    .jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
            }.getOrNull().orEmpty()
            val code = message.substringBefore(' ').substringBefore(':').trim()
            return when (code) {
                "EMAIL_EXISTS" -> AuthApiException.EmailExists()
                "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND", "USER_DISABLED",
                "INVALID_REFRESH_TOKEN", "TOKEN_EXPIRED", "USER_NOT_FOUND" -> AuthApiException.InvalidCredentials(code)
                "WEAK_PASSWORD" -> AuthApiException.WeakPassword()
                "TOO_MANY_ATTEMPTS_TRY_LATER" -> AuthApiException.TooManyAttempts()
                "CONFIGURATION_NOT_FOUND", "OPERATION_NOT_ALLOWED", "PASSWORD_LOGIN_DISABLED" -> AuthApiException.NotConfigured(code)
                else -> AuthApiException.Other(code.ifBlank { "UNKNOWN" })
            }
        }
    }
}

/** Adds the platform app identity headers required by restricted API keys. */
internal fun HttpRequestBuilder.appIdentity(config: FirebaseConfig) {
    config.iosBundleId?.let { header("X-Ios-Bundle-Identifier", it) }
    config.androidPackage?.let { header("X-Android-Package", it) }
    config.androidCertSha1?.let { header("X-Android-Cert", it) }
}
