@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application") apply true
    id("kotlin-android") apply true
}

android {
    namespace = "com.github.hecodes2much.mlauncher"
    compileSdk = 33

    defaultConfig {
        applicationId = "app.mlauncher"
        minSdk = 23
        targetSdk = 33
        versionCode = 52
        versionName = "0.5.2"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue ("string", "app_name", "mLauncher Debug")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue ("string", "app_name", "mLauncher")
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName = "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Signed.apk"
                }
            }
        }
        if (buildType.name == "debug") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName = "${defaultConfig.applicationId}_v${defaultConfig.versionName}-Debug.apk"
                }
            }
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_9
        targetCompatibility = JavaVersion.VERSION_1_9
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    val kotlinVersion: String? by extra

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // Android lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // JETPACK
    // Integration with activities
    //noinspection GradleDependency
    implementation("androidx.activity:activity-compose:1.6.1")
    // Compose Material Design
    implementation("androidx.compose.material:material:1.3.1")
    implementation("com.github.SmartToolFactory:Compose-Colorful-Sliders:1.1.0")
    // Animations
    implementation("androidx.compose.animation:animation:1.3.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // AndroidX
    implementation("androidx.compose.ui:ui:1.3.3")
    implementation("androidx.compose.ui:ui-tooling:1.3.3")
    implementation("androidx.compose.foundation:foundation:1.3.1")

    implementation("com.google.code.gson:gson:2.10.1")

    val androidxTestEspresso = "3.5.1"
    androidTestImplementation("androidx.test.espresso:espresso-core:$androidxTestEspresso")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$androidxTestEspresso")
    implementation("androidx.test.espresso:espresso-idling-resource:$androidxTestEspresso")
    implementation("androidx.test.espresso:espresso-idling-resource:$androidxTestEspresso")

    // Test rules and transitive dependencies:
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.3.3")
    // Needed for createComposeRule, but not createAndroidComposeRule:
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.3.3")
    androidTestImplementation("androidx.navigation:navigation-testing:2.5.3")
    debugImplementation("androidx.fragment:fragment-testing:1.5.5")
    implementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
