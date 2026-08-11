// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.cyclonedx.bom") version "3.3.0"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Scan only what ships in the APK. Test/tooling classpaths
// (Robolectric, AGP UTP, lint) pull netty/grpc/bouncycastle and
// produce false positives in the DevSecOps vulnerability scan.
subprojects {
    tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
        includeConfigs = listOf("releaseRuntimeClasspath")
    }
}

group = "com.bioguard"
version = "1.0.0"
