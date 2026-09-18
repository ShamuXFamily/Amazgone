package com.cikup.amazgone.core.remote

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Reads values generated at build time from androidApp/google-services.json
 * (see androidApp/build.gradle.kts); null when the file was not provided (local-only mode).
 */
fun loadFirebaseConfig(context: Context): FirebaseConfig? {
    fun value(name: String): String? {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        return if (id == 0) null else context.getString(id).ifBlank { null }
    }
    val apiKey = value("amazgone_firebase_api_key") ?: return null
    val projectId = value("amazgone_firebase_project_id") ?: return null
    return FirebaseConfig(
        apiKey = apiKey,
        projectId = projectId,
        androidPackage = context.packageName,
        androidCertSha1 = signingCertSha1(context),
    )
}

/** Android-restricted Google API keys verify the caller by package name + signing cert SHA-1. */
@Suppress("DEPRECATION")
private fun signingCertSha1(context: Context): String? = runCatching {
    val pm = context.packageManager
    val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
    } ?: return null
    MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()).joinToString("") { "%02X".format(it) }
}.getOrNull()
