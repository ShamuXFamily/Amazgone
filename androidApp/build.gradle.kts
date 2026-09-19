import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.cikup.amazgone"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cikup.amazgone"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        firebaseValuesFrom(file("google-services.json")).forEach { (name, value) -> resValue("string", name, value) }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

/**
 * Extracts the Firebase API key and project id from google-services.json (gitignored) so the
 * shared REST client can talk to Firebase, plus the values Firebase Analytics needs to start. Without the file the app runs in local-only mode.
 */
fun firebaseValuesFrom(json: File): Map<String, String> {
    if (!json.exists()) return emptyMap()
    @Suppress("UNCHECKED_CAST")
    val root = JsonSlurper().parse(json) as Map<String, Any?>
    val projectId = (root["project_info"] as Map<String, Any?>)["project_id"] as String
    val client = (root["client"] as List<Map<String, Any?>>).first()
    val apiKey = ((client["api_key"] as List<Map<String, Any?>>).first())["current_key"] as String
    val project = root["project_info"] as Map<String, Any?>
    val appId = (client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"] as String
    return mapOf(
        "amazgone_firebase_api_key" to apiKey,
        "amazgone_firebase_project_id" to projectId,
        // Standard names the google-services plugin would generate; FirebaseInitProvider reads them at start-up
        // (needed by the Firebase Analytics SDK). Without google-services.json none are set and analytics is off.
        "google_app_id" to appId,
        "google_api_key" to apiKey,
        "project_id" to projectId,
        "gcm_defaultSenderId" to project["project_number"].toString(),
        "google_storage_bucket" to (project["storage_bucket"] as String? ?: ""),
    )
}
