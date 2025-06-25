package com.github.droidworksstudio.mlauncher.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.CrashHandler
import java.lang.ref.WeakReference

class ActionService : AccessibilityService() {
    private var suppressEventsUntil = 0L

    override fun onServiceConnected() {
        instance = WeakReference(this)
        AppLogger.d("ActionService", "Service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AppLogger.d("ActionService", "Service unbound")
        instance = WeakReference(null)
        return super.onUnbind(intent)
    }

    private var lastActionTime = 0L
    private val actionCooldown = 1500L  // cooldown in milliseconds

    private fun canPerformAction(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionCooldown) {
            AppLogger.d("ActionService", "Action called too quickly; ignoring.")
            return false
        }
        lastActionTime = now
        suppressEventsUntil = now + actionCooldown
        return true
    }

    fun lockScreen(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Lock Screen")
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun showRecents(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Show Recents")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Open Notifications")
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Open Quick Settings")
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun openPowerDialog(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Open Power Dialog")
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    fun takeScreenShot(): Boolean {
        if (!canPerformAction()) return false
        CrashHandler.logUserAction("Take Screen Shot")
        return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        AppLogger.d("ActionService", "Service interrupted")
    }

    companion object {
        private var instance: WeakReference<ActionService> = WeakReference(null)

        fun instance(): ActionService? {
            return instance.get()
        }
    }
}
