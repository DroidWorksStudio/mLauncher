import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application") apply true
    id("kotlin-android") apply true
}


android {
    namespace = "app.mlauncher"
    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    defaultConfig {
        applicationId = "app.mlauncher"
        minSdk = 26
        targetSdk = 33
        versionCode = 40
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    buildTypes {
        named("release") {
            isDebuggable = false
            isMinifyEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
        named("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                if (output?.outputFileName?.endsWith(".apk") == true) {
                    output.outputFileName = "${defaultConfig.applicationId}_v${defaultConfig.versionName}.apk"
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }


    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    // Android lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.code.gson:gson:2.8.9")

    // JETPACK
    // Integration with activities
    //noinspection GradleDependency
    implementation("androidx.activity:activity-compose:1.6.1")
    // Compose Material Design
    implementation("androidx.compose.material:material:1.3.1")
    // Animations
    implementation("androidx.compose.animation:animation:1.3.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}
