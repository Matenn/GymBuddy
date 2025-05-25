plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.compose.compiler)

    id("com.google.dagger.hilt.android") // Updated Hilt plugin ID
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kaczmarzykmarcin.GymBuddy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kaczmarzykmarcin.GymBuddy"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
        compose = true
    }
}

dependencies {

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.facebook.android.sdk)
    // Firebase Authentication - do logowania użytkowników
    implementation(libs.firebase.auth.ktx)

    // Firebase Firestore - do przechowywania danych
    implementation(libs.firebase.firestore.ktx)

    // Firebase Storage - do przechowywania zdjęć (opcjonalnie)
    implementation(libs.firebase.storage.ktx)

    // Firebase Analytics - śledzenie użycia aplikacji (opcjonalnie)
    implementation(libs.firebase.analytics.ktx)

    // Google Play services Auth - potrzebne do logowania przez Google
    implementation(libs.play.services.auth)


    // Firebase

    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.google.firebase.storage.ktx)

    // Google Sign-In
    //implementation(libs.play.services.auth.v2070)

    // Retrofit dla ExerciseDB API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // Coil dla ładowania obrazów
    implementation(libs.coil.compose)

    // Biblioteka do wykresów - Vico
    implementation("com.patrykandpatrick.vico:compose:2.1.2")
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.2")
    implementation("com.patrykandpatrick.vico:core:2.1.2")
    implementation("com.patrykandpatrick.vico:views:2.1.2")
    // ViewModel dla Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Accompanist (pomocnicze biblioteki dla Compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    implementation(libs.androidx.runtime)
    // You may also need these core Compose dependencies
    implementation(libs.androidx.ui)
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))

    implementation("androidx.compose.foundation:foundation:1.8.0")
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation (libs.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    implementation(libs.play.services.maps)

    implementation(libs.facebook.login)

    // Gson dla parsowania JSON
    implementation(libs.gson)

    // Coil dla wczytywania obrazów (jeśli używasz Jetpack Compose)
    implementation(libs.coil)
    implementation(libs.coil.compose)



    implementation(libs.androidx.room.runtime)
    implementation (libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


