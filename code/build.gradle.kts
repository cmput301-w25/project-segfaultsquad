// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // Add the dependency for the Google services Gradle plugin (For Firebase/Google Maps API)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Add the classpath for Firebase and Google services
        classpath("com.google.gms:google-services:4.4.2")
    }
}