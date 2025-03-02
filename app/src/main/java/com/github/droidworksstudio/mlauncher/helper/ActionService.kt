package com.github.droidworksstudio.mlauncher.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.github.droidworksstudio.common.CrashHandler
import java.lang.ref.WeakReference

class ActionService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = WeakReference(null)
        return super.onUnbind(intent)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun lockScreen(): Boolean {
        CrashHandler.logUserAction("Lock Screen")
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showRecents(): Boolean {
        CrashHandler.logUserAction("Show Recents")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun openNotifications(): Boolean {
        CrashHandler.logUserAction("Open Notifications")
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun openQuickSettings(): Boolean {
        CrashHandler.logUserAction("Open Quick Settings")
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun openPowerDialog(): Boolean {
        CrashHandler.logUserAction("Open Power Dialog")
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun takeScreenShot(): Boolean {
        CrashHandler.logUserAction("Take Screen Shot")
        return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        private var instance: WeakReference<ActionService> = WeakReference(null)

        fun instance(): ActionService? {
            return instance.get()
        }
    }
}