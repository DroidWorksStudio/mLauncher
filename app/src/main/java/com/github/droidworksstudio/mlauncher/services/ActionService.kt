package com.github.droidworksstudio.mlauncher.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
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

    fun lockScreen(): Boolean {
        CrashHandler.logUserAction("Lock Screen")
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun showRecents(): Boolean {
        CrashHandler.logUserAction("Show Recents")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        CrashHandler.logUserAction("Open Notifications")
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        CrashHandler.logUserAction("Open Quick Settings")
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun openPowerDialog(): Boolean {
        CrashHandler.logUserAction("Open Power Dialog")
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

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