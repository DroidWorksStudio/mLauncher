@file:Suppress("DEPRECATION")

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlinVersion = "1.8.10"
    extra ["kotlinVersion"] = kotlinVersion
    dependencies {
        classpath(kotlin("gradle-plugin", version = kotlinVersion))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    // Make sure that you have the Google services Gradle plugin dependency
    id("com.google.gms.google-services") version "4.4.1" apply false
    // Add the dependency for the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
 }
