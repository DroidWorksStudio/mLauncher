package app.mlauncher.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
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
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { }

    companion object {
        private var instance: WeakReference<ActionService> = WeakReference(null)

        fun instance(): ActionService? {
            return instance.get()
        }

    }
}