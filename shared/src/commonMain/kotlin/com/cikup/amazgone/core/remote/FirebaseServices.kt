package com.cikup.amazgone.core.remote

/** Null members mean Firebase is not configured: the app keeps working local-only. */
class FirebaseServices(
    val auth: FirebaseAuthApi?,
    val firestore: FirestoreClient?,
) {
    val isAvailable: Boolean get() = auth != null && firestore != null

    fun requireFirestore(): FirestoreClient =
        firestore ?: throw FirestoreException.Unavailable("Firebase is not configured")
}
