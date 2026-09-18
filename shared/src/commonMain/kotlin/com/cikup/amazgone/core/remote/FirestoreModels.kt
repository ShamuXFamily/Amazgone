package com.cikup.amazgone.core.remote


/** A fetched document: [id] is the last path segment, [fields] already decoded. */
data class FirestoreDocument(
    val path: String,
    val fields: Map<String, Any?>,
    val updateTime: String?,
) {
    val id: String get() = path.substringAfterLast('/')
}

/** One write inside an atomic commit. Paths are relative to the database root (e.g. "users/abc"). */
sealed interface FirestoreWrite {
    /** Creates or overwrites; [mask] limits which fields are replaced; [precondition] guards races/idempotency. */
    data class Set(
        val path: String,
        val fields: Map<String, Any?>,
        val mask: List<String>? = null,
        val precondition: Precondition? = null,
        /** Applied atomically in the same write, so security rules see fields + transforms together. */
        val transforms: List<FieldTransform> = emptyList(),
    ) : FirestoreWrite

    data class Delete(val path: String, val precondition: Precondition? = null) : FirestoreWrite

    /** Atomic server-side field transforms (increment, server timestamp). */
    data class Transform(val path: String, val transforms: List<FieldTransform>) : FirestoreWrite
}

sealed interface Precondition {
    data class Exists(val exists: Boolean) : Precondition
    /** Optimistic concurrency: fails if the document changed since it was read. */
    data class UpdateTime(val updateTime: String) : Precondition
}

sealed interface FieldTransform {
    val field: String
    data class Increment(override val field: String, val by: Long) : FieldTransform
    data class ServerTimestamp(override val field: String) : FieldTransform
}

/** Firestore REST errors we act on; everything else is transient. */
sealed class FirestoreException(message: String) : Exception(message) {
    class PreconditionFailed(message: String) : FirestoreException(message)
    class PermissionDenied(message: String) : FirestoreException(message)
    class NotFound(message: String) : FirestoreException(message)
    class Aborted(message: String) : FirestoreException(message)
    class Unauthenticated(message: String) : FirestoreException(message)
    class Unavailable(message: String) : FirestoreException(message)
}

