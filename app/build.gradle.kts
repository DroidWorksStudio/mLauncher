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
        versionCode = 168
        versionName = "1.6.8"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "mLauncher Debug")
        }

        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    val androidxTestKotlin = "1.7.5"
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Android lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")

    // Text similarity
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // JETPACK
    // Integration with activities
    //noinspection GradleDependency
    implementation("androidx.activity:activity-compose:1.9.1")
    // Compose Material Design
    implementation("androidx.compose.material:material:$androidxTestKotlin")
    implementation("com.github.SmartToolFactory:Compose-Colorful-Sliders:1.2.0")
    // Animations
    implementation("androidx.compose.animation:animation:$androidxTestKotlin")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")

    // AndroidX
    implementation("androidx.compose.ui:ui:$androidxTestKotlin")
    implementation("androidx.compose.ui:ui-tooling:$androidxTestKotlin")
    implementation("androidx.compose.foundation:foundation:$androidxTestKotlin")
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")

    //color picker
    implementation("net.mm2d.color-chooser:color-chooser:0.7.3")

    val acraVersion = "5.11.4"
    implementation("ch.acra:acra-core:$acraVersion")
    implementation("ch.acra:acra-dialog:$acraVersion")
    implementation("ch.acra:acra-mail:$acraVersion")

    val androidxTestEspresso = "3.6.1"
    androidTestImplementation("androidx.test.espresso:espresso-core:$androidxTestEspresso")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$androidxTestEspresso")
    implementation("androidx.test.espresso:espresso-idling-resource:$androidxTestEspresso")
    implementation("androidx.test.espresso:espresso-idling-resource:$androidxTestEspresso")

    // Test rules and transitive dependencies:
   androidTestImplementation("androidx.compose.ui:ui-test-junit4:$androidxTestKotlin")
    // Needed for createComposeRule, but not createAndroidComposeRule:
    debugImplementation("androidx.compose.ui:ui-test-manifest:$androidxTestKotlin")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.4")
    debugImplementation("androidx.fragment:fragment-testing:1.8.5")
    implementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
