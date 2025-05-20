package com.github.droidworksstudio.mlauncher.helper.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.app.ActivityCompat

object AppReloader {
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)

        context.startActivity(mainIntent)

        when (context) {
            is Activity -> ActivityCompat.finishAffinity(context)
            is ContextWrapper -> {
                val base = context.baseContext
                if (base is Activity) ActivityCompat.finishAffinity(base)
            }
        }
    }

}

