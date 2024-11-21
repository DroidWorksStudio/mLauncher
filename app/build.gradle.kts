@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application") apply true
    id("kotlin-android") apply true
}

android {
    namespace = "com.github.droidworksstudio.mlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.mlauncher"
        minSdk = 23
        targetSdk = 35
        versionCode = 170
        versionName = "1.7.0"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "mLauncher Debug")
        }

        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "mLauncher")
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName =
                        "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Signed.apk"
                }
            }
        }
        if (buildType.name == "debug") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName =
                        "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Debug.apk"
                }
            }
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    // Core libraries
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)

    // Android Lifecycle
    implementation(libs.lifecycle.extensions)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Work Manager
    implementation(libs.work.runtime.ktx)

    // UI Components
    implementation(libs.constraintlayout)
    implementation(libs.constraintlayout.compose)
    implementation(libs.activity.compose)

    // Jetpack Compose
    implementation(libs.compose.material) // Compose Material Design
    implementation(libs.compose.animation) // Animations
    implementation(libs.compose.ui) // Core UI library
    implementation(libs.compose.foundation) // Foundation library
    implementation(libs.compose.ui.tooling) // UI tooling for previews

    // Text similarity and JSON handling
    implementation(libs.commons.text)
    implementation(libs.gson)

    // Color picker and sliders
    implementation(libs.color.chooser) // Simple color picker
    implementation(libs.compose.colorful.sliders) // Compose colorful sliders

    // Biometric support
    implementation(libs.biometric.ktx)

    // ACRA for crash reporting
    implementation(libs.acra.core)
    implementation(libs.acra.dialog)
    implementation(libs.acra.mail)

    // AndroidX Test - Espresso
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    implementation(libs.espresso.idling.resource) // Idling resources for Espresso tests

    // Test rules and other testing dependencies
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    implementation(libs.test.core.ktx) // Test core utilities

    // Jetpack Compose Testing
    androidTestImplementation(libs.ui.test.junit4) // For createComposeRule
    debugImplementation(libs.ui.test.manifest) // Debug-only dependencies for Compose testing

    // Fragment testing
    debugImplementation(libs.fragment.testing)

    // Navigation testing
    androidTestImplementation(libs.navigation.testing)
}
