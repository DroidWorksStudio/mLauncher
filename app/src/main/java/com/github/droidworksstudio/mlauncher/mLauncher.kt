package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.data.Prefs

class Mlauncher : Application() {
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()

        // Initialize com.github.droidworksstudio.common.CrashHandler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        // Initialize prefs here
        prefs = Prefs(applicationContext)

        setCustomFont(applicationContext)

        // Log app launch
        CrashHandler.logUserAction("App Launched")
    }


    private fun setCustomFont(context: Context) {
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
