package com.github.droidworksstudio.mlauncher.helper.utils

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppReloader {
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)

        // Use a lifecycle-aware global scope for the app
        val appScope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope

        appScope.launch(Dispatchers.Main) {
            delay(100)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0) // Forcefully terminates the current process
        }
    }
}

