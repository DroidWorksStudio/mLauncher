package com.github.droidworksstudio.launcher.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity.ACCESSIBILITY_SERVICE
import com.github.droidworksstudio.launcher.R
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager

class GestureUtils(private val context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)

    fun getSwipeInfo(launcherApps: LauncherApps, direction: String): Pair<LauncherActivityInfo?, Int?> {
        val app = sharedPreferenceManager.getGestureInfo(direction)
        println(app)
        if (app != null) {
            if (app.size >= 3) {
                val componentName = ComponentName.unflattenFromString(app[1])
                if (componentName != null) {
                    return Pair(
                        launcherApps.resolveActivity(
                            Intent().setComponent(componentName), launcherApps.profiles[app[2]
                                .toInt()]
                        ), app[2].toInt()
                    )
                }
            }
        }
        return Pair(null, null)
    }

    fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(context.packageName) && enabledServiceInfo.name.equals(
                    service.name
                )
            ) return true
        }

        return false
    }

    fun promptEnableAccessibility() {
        AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.confirm_title))
            setMessage(context.getString(R.string.screenlock_confirmation))
            setPositiveButton(context.getString(R.string.confirm_yes)) { _, _ ->
                // Perform action on confirmation
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            setNegativeButton(context.getString(R.string.confirm_no)) { _, _ ->

            }

        }.create().show()
    }
}