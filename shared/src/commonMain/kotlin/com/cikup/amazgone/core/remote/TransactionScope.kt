package com.cikup.amazgone.core.remote

/**
 * Reads made during [FirestoreClient.runTransaction]. Each read document's version is remembered so
 * the commit can require it to be unchanged (or still missing) — optimistic concurrency.
 */
class TransactionScope internal constructor(private val client: FirestoreClient) {
    private val versions = mutableMapOf<String, String?>()

    suspend fun get(path: String): FirestoreDocument? =
        client.get(path).also { versions[path] = it?.updateTime }

    /** Adds preconditions for pinned documents; writes that already carry one keep it. */
    internal fun pin(writes: List<FirestoreWrite>): List<FirestoreWrite> = writes.map { write ->
        if (write !is FirestoreWrite.Set || write.precondition != null || write.path !in versions) return@map write
        val version = versions.getValue(write.path)
        write.copy(precondition = if (version == null) Precondition.Exists(false) else Precondition.UpdateTime(version))
    }
}
