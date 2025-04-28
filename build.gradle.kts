buildscript {
    repositories{
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0") // lub aktualna wersja

        classpath("com.google.gms:google-services:4.3.15") // plugin dla Firebase
        classpath(libs.kotlin.gradle.plugin) // Use your Kotlin version
        classpath(libs.androidx.compiler)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.dagger.hilt.android") version "2.56" apply false


    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false

}