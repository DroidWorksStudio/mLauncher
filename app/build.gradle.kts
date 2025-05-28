plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.android)
}

// Top of build.gradle.kts
val major = 1
val minor = 11
val patch = 0
val build = 0

val type = 1 // 1=beta, 2=alpha else=production

val baseVersionName = "$major.$minor.$patch.$build"

val versionCodeInt =
    (String.format("%02d", major) + String.format("%02d", minor) + String.format(
        "%02d",
        patch
    ) + String.format("%02d", build)).toInt()

val versionNameStr = when (type) {
    1 -> "$baseVersionName-beta"
    2 -> "$baseVersionName-alpha"
    else -> baseVersionName
}

android {
    namespace = "com.github.droidworksstudio.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.mlauncher"
        minSdk = 30
        targetSdk = 36
        versionCode = versionCodeInt
        versionName = versionNameStr

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".dev"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Multi Launcher Dev")
            resValue("string", "app_version", versionNameStr)
            resValue("string", "empty", "")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "Multi Launcher")
            resValue("string", "app_version", versionNameStr)
            resValue("string", "empty", "")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.preference.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.biometric.ktx)
}