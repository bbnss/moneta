plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Modulo Kotlin puro: nessuna dipendenza da Android, così i test girano sulla JVM
// in millisecondi e la logica monetaria resta verificabile senza emulatore.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
