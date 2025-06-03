package com.github.droidworksstudio.mlauncher

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.IconCacheTarget
import com.github.droidworksstudio.mlauncher.helper.IconPackHelper
import java.io.File
import java.util.concurrent.Executors

class Mlauncher : Application() {
    companion object {
        private var appContext: Context? = null

        fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "Mlauncher not initialized. Ensure Mlauncher.initialize(context) is called early."
            )
        }

        fun initialize(context: Context) {
            if (appContext != null) return // already initialized
            appContext = context.applicationContext

            // Optional: preload icons, init crash handler, etc. if needed
            val prefs = Prefs(appContext!!)
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                if (prefs.iconPackHome == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(appContext!!, prefs.customIconPackHome, IconCacheTarget.HOME)
                }

                if (prefs.iconPackAppList == Constants.IconPacks.Custom) {
                    IconPackHelper.preloadIcons(appContext!!, prefs.customIconPackAppList, IconCacheTarget.APP_LIST)
                }
            }

            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(appContext!!))

            setCustomFont(appContext!!)

            CrashHandler.logUserAction("App Launched")
        }

        private fun setCustomFont(context: Context) {
            val prefs = Prefs(context)
            val customFontFile = File(context.filesDir, "CustomFont.ttf")
            if (!customFontFile.exists()) {
                prefs.fontFamily = Constants.FontFamily.System
            }

            val customFont = prefs.fontFamily.getFont(context)
            if (customFont != null) {
                TypefaceUtil.setDefaultFont("DEFAULT", customFont)
                TypefaceUtil.setDefaultFont("MONOSPACE", customFont)
                TypefaceUtil.setDefaultFont("SERIF", customFont)
                TypefaceUtil.setDefaultFont("SANS_SERIF", customFont)
            }
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
