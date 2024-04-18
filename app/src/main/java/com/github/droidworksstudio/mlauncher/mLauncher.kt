package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.Context
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.acra.ReportField
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra


class Mlauncher : Application() {
    private lateinit var prefs: Prefs

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Initialize prefs here
        prefs = Prefs(base)

        val pkgName = getString(R.string.app_name)

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            reportContent = listOf(
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT
            )
            //each plugin you chose above can be configured in a block like this:
            toast {
                enabled = true
                //required
                text = getString(R.string.acra_toast_text).format(pkgName)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val applicationId = BuildConfig.FIREBASE_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY

        // Manually initialize Firebase with configuration values
        val options = FirebaseOptions.Builder()
            .setProjectId("mlauncher-android")
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .build()
        initializeFirebase(options)

        // Enable Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    private fun initializeFirebase(options: FirebaseOptions) {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }
    }

}
