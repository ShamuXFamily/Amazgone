package com.cikup.amazgone.core.common

import kotlin.uuid.Uuid

/** Generates globally unique ids; also used as idempotency keys for remote writes. */
fun interface IdGenerator {
    fun newId(): String
}

object UuidGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
