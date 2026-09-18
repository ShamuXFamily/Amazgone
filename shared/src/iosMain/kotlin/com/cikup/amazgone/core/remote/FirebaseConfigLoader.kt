package com.cikup.amazgone.core.remote

import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.dictionaryWithContentsOfFile

/** Reads GoogleService-Info.plist from the app bundle; null when the file is missing (local-only mode). */
fun loadFirebaseConfig(): FirebaseConfig? {
    val path = NSBundle.mainBundle.pathForResource("GoogleService-Info", ofType = "plist") ?: return null
    val plist = NSDictionary.dictionaryWithContentsOfFile(path) ?: return null
    val apiKey = plist["API_KEY"] as? String ?: return null
    val projectId = plist["PROJECT_ID"] as? String ?: return null
    return FirebaseConfig(apiKey, projectId, iosBundleId = plist["BUNDLE_ID"] as? String)
}
