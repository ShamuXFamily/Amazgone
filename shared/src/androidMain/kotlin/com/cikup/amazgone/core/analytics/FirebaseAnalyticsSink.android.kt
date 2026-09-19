package com.cikup.amazgone.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/** Android: forwards events to the Firebase Analytics SDK. */
class FirebaseAnalyticsSink private constructor(private val firebase: FirebaseAnalytics) : AnalyticsSink {

    override fun logEvent(name: String, params: Map<String, Any>) = firebase.logEvent(name, params.toBundle())

    override fun setUserId(id: String?) = firebase.setUserId(id)

    override fun setUserProperty(name: String, value: String?) = firebase.setUserProperty(name, value)

    private fun Map<String, Any>.toBundle() = Bundle().also { bundle ->
        forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Double -> bundle.putDouble(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
    }

    companion object {
        /** Null when google-services values weren't bundled (Firebase never initialised). */
        fun createOrNull(context: Context): AnalyticsSink? =
            if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAnalyticsSink(FirebaseAnalytics.getInstance(context))
    }
}
