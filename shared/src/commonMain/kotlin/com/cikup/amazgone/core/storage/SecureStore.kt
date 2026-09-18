package com.cikup.amazgone.core.storage

/** Small secret storage (auth refresh token). Keychain on iOS, EncryptedSharedPreferences on Android. */
interface SecureStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

/** Test/in-memory implementation. */
class InMemorySecureStore : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
