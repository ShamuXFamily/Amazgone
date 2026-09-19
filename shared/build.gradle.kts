import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    // JVM target exists for fast host tests (Room/Ktor/domain) and honest Kover coverage; no desktop app ships.
    jvm()

    android {
       namespace = "com.cikup.amazgone.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    compilerOptions {
        optIn.addAll(
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
        )
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.security.crypto)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.compose.material3.navigationSuite)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.datetime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
        // Database/integration tests need a real Room database: shared by iOS and JVM (no Android Context needed).
        iosTest { kotlin.srcDir("src/dbTest/kotlin") }
        jvmTest { kotlin.srcDir("src/dbTest/kotlin") }
        getByName("androidHostTest").dependencies {
            implementation(libs.konsist)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64", "kspJvm").forEach { add(it, libs.room.compiler) }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kover {
    reports {
        filters {
            excludes {
                // Compose UI, DI wiring, platform glue and generated code are verified on-device
                // (iOS Simulator first), not by unit-test coverage.
                annotatedBy("androidx.compose.runtime.Composable")
                packages("amazgone.shared.generated.resources")
                classes(
                    "*_Impl", "*_Impl\$*", "*Constructor", "*.AppDatabase\$*",
                    "*ScreenKt*", "*ComponentsKt*", "*SectionsKt*", "*StepsKt*", "*RowKt*", "*CardKt*",
                    "*Kt\$*\$lambda*", "*ComposableSingletons*",
                )
                packages(
                    "com.cikup.amazgone.navigation",
                    "com.cikup.amazgone.core.designsystem.component",
                    "com.cikup.amazgone.core.designsystem.image",
                    "com.cikup.amazgone.core.designsystem.theme",
                    "com.cikup.amazgone.core.presentation.error",
                    "com.cikup.amazgone.progress.presentation",
                )
                classes("*.di.*", "*.core.sync.*ConnectivityObserver", "*.core.sync.SyncWorker*", "*.core.storage.*SecureStore", "*FirebaseConfigLoader*", "*PlatformModule*", "*DatabaseBuilder*", "*ReduceMotion*", "*ImageCache*")
            }
        }
        verify {
            rule { minBound(80) }
        }
    }
}
