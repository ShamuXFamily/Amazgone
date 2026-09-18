package com.cikup.amazgone.core.database

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Runs several DAO writes atomically (e.g. cart change + outbox row).
 * Repositories depend on this interface so tests can use [NoTransaction].
 */
interface TransactionRunner {
    suspend fun <R> inTransaction(block: suspend () -> R): R
}

class RoomTransactionRunner(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <R> inTransaction(block: suspend () -> R): R =
        database.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}

object NoTransaction : TransactionRunner {
    override suspend fun <R> inTransaction(block: suspend () -> R): R = block()
}
