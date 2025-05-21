package com.github.droidworksstudio.mlauncher

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.IconCacheTarget
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper
import java.io.File

class Mlauncher : Application() {
    private lateinit var prefs: Prefs

    companion object {
        // Directly store the application context
        private var appContext: Context? = null

        // Access the context directly without WeakReference
        fun getContext(): Context {
            return appContext ?: throw IllegalStateException("Context is not initialized.")
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize appContext here
        appContext = applicationContext

        // Initialize prefs here
        prefs = Prefs(applicationContext)

        if (prefs.iconPackHome == Constants.IconPacks.Custom) {
            IconPackHelper.preloadIcons(this, prefs.customIconPackHome, IconCacheTarget.HOME)
        }

        if (prefs.iconPackAppList == Constants.IconPacks.Custom) {
            IconPackHelper.preloadIcons(this, prefs.customIconPackAppList, IconCacheTarget.APP_LIST)
        }

        // Initialize com.github.droidworksstudio.common.CrashHandler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        setCustomFont(applicationContext)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val isLocked = prefs.lockOrientation

                activity.requestedOrientation = if (isLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // Log app launch
        CrashHandler.logUserAction("App Launched")
    }


    private fun setCustomFont(context: Context) {
        val customFontFile = File(context.filesDir, "CustomFont.ttf")
        if (!customFontFile.exists()) {
            prefs.fontFamily = Constants.FontFamily.System
        }

        // Load the custom font from resources
        val customFont = prefs.fontFamily.getFont(context)

        // Apply the custom font to different font families
        if (customFont != null) {
            TypefaceUtil.setDefaultFont("DEFAULT", customFont)
            TypefaceUtil.setDefaultFont("MONOSPACE", customFont)
            TypefaceUtil.setDefaultFont("SERIF", customFont)
            TypefaceUtil.setDefaultFont("SANS_SERIF", customFont)
        }
    }
}

object TypefaceUtil {

    fun setDefaultFont(staticTypefaceFieldName: String, fontAssetName: Typeface) {
        Log.e("setDefaultFont", "$staticTypefaceFieldName | $fontAssetName")
        try {
            val staticField = Typeface::class.java.getDeclaredField(staticTypefaceFieldName)
            staticField.isAccessible = true
            staticField.set(null, fontAssetName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
