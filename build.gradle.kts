// Kotlin 2.4 requires R8 9.1.29. Keep AGP 8 while using the compatible shrinker.
// https://developer.android.com/build/kotlin-support
buildscript {
    repositories {
        maven("https://storage.googleapis.com/r8-releases/raw") { content { includeModule("com.android.tools", "r8") } }
        google()
        mavenCentral()
    }
    dependencies { classpath("com.android.tools:r8:9.1.29") }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
