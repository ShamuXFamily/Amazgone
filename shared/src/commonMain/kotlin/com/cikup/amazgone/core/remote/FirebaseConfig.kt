package com.cikup.amazgone.core.remote

/**
 * Client identifiers for the Firebase project (not secrets; access is enforced by security rules).
 * Loaded per platform from GoogleService-Info.plist (iOS) / google-services.json (Android).
 */
data class FirebaseConfig(
    val apiKey: String,
    val projectId: String,
    /** Sent as X-Ios-Bundle-Identifier when the API key is restricted to the iOS app. */
    val iosBundleId: String? = null,
    val androidPackage: String? = null,
    /** Upper-case hex SHA-1 of the signing certificate, sent as X-Android-Cert for Android-restricted keys. */
    val androidCertSha1: String? = null,
)

/** Koin cannot hold null singletons; absent config means the app runs local-only. */
data class FirebaseConfigHolder(val config: FirebaseConfig?)
