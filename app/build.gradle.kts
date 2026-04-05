plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.app.lumen"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bajpro.lumen"
        minSdk = 28
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)

    // RevenueCat
    implementation(libs.revenuecat)
    implementation(libs.revenuecat.ui)

    // Mixpanel
    implementation(libs.mixpanel)

    // Coil (image loading - equivalent of Kingfisher on iOS)
    implementation(libs.coil.compose)

    // Facebook SDK
    implementation(libs.facebook.android.sdk)

    // Google Generative AI (Gemini)
    implementation(libs.generative.ai)

    // Room (local database - equivalent of SwiftData on iOS)
    // TODO: Re-enable Room + KSP when compatible with AGP 9.x built-in Kotlin
    // Requires Kotlin 2.3.0+ and KSP 2.3.4+
    // implementation(libs.room.runtime)
    // implementation(libs.room.ktx)
    // ksp(libs.room.compiler)

    // DataStore (equivalent of UserDefaults on iOS)
    implementation(libs.datastore.preferences)

    // Media3 / ExoPlayer (audio playback - equivalent of AVPlayer on iOS)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Glance (App Widgets with Compose)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // WorkManager
    implementation(libs.work.runtime)

    // Google Play In-App Review
    implementation(libs.play.review)

    // Splash Screen
    implementation(libs.splash.screen)

    // Floating Tab Bar
    implementation(libs.floating.tab.bar)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
