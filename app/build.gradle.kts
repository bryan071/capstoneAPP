plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.project.webapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.project.webapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // GCash API credentials
        buildConfigField("String", "GCASH_CLIENT_ID", "\"your_gcash_client_id_here\"")
        buildConfigField("String", "GCASH_CLIENT_SECRET", "\"your_gcash_client_secret_here\"")
        buildConfigField("String", "GCASH_MERCHANT_ID", "\"your_gcash_merchant_id_here\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // Enable BuildConfig generation
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.0")
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx:23.3.1")
    implementation("com.google.firebase:firebase-auth")


    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.android.play:integrity:1.4.0")

    // Jetpack Compose
    implementation("androidx.compose:compose-bom:2024.02.00")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material:1.5.4")
    implementation ("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation ("androidx.compose.material3:material3:1.1.2")

    // ViewModel Support
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.31.3-beta")
    implementation ("com.google.accompanist:accompanist-pager:0.32.0")
    implementation ("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    // Weather API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.android.gms:play-services-tasks:18.0.2")
    implementation("com.google.android.gms:play-services-location:21.0.1") // For GPS
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Google Maps API
    implementation ("com.google.accompanist:accompanist-permissions:0.33.1-alpha")
    implementation ("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")

    implementation ("com.google.zxing:core:3.4.1") // ZXing dependency for GCASH

    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
// Kotlin Coroutines for Flow and Firestore await()
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    val nav_version = "2.8.7"
    implementation("androidx.navigation:navigation-compose:$nav_version")
}