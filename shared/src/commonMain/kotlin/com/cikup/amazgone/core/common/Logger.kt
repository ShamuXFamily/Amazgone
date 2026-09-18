package com.cikup.amazgone.core.common

/** Minimal logging seam: detailed context goes to logs, never to the UI. */
interface AppLogger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

object PrintLogger : AppLogger {
    override fun debug(tag: String, message: String) = println("D/$tag: $message")
    override fun error(tag: String, message: String, throwable: Throwable?) =
        println("E/$tag: $message${throwable?.let { " — ${it::class.simpleName}: ${it.message}" } ?: ""}")
}
