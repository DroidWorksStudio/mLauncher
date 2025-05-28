package com.github.droidworksstudio.launcher.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.widget.Toast
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppUtils(private val context: Context, private val launcherApps: LauncherApps) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)

    suspend fun getInstalledApps(): List<Triple<LauncherActivityInfo, UserHandle, Int>> {
        val allApps = mutableListOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
        var sortedApps = listOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
        withContext(Dispatchers.Default) {
            for (i in launcherApps.profiles.indices) { // Check apps on both, normal and work profiles
                launcherApps.getActivityList(null, launcherApps.profiles[i]).forEach { app ->
                    if (!sharedPreferenceManager.isAppHidden( // Only include the app if it isn't set as hidden
                            app.applicationInfo.packageName,
                            i
                        ) && app.applicationInfo.packageName != context.applicationInfo.packageName // Hide the launcher itself
                    ) {
                        allApps.add(Triple(app, launcherApps.profiles[i], i)) // The i variable gets used to determine whether an app is in the personal profile or work profile
                    }
                }
            }

            // Sort apps by name
            sortedApps = allApps.sortedBy {
                sharedPreferenceManager.getAppName(
                    it.first.applicationInfo.packageName,
                    it.third,
                    context.packageManager.getApplicationLabel(it.first.applicationInfo)
                ).toString().lowercase()
            }
        }
        return sortedApps

    }

    // Get hidden apps for the hidden apps settings
    suspend fun getHiddenApps(): List<Triple<LauncherActivityInfo, UserHandle, Int>> {
        val allApps = mutableListOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
        var sortedApps = listOf<Triple<LauncherActivityInfo, UserHandle, Int>>()
        withContext(Dispatchers.Default) {
            for (i in launcherApps.profiles.indices) {
                launcherApps.getActivityList(null, launcherApps.profiles[i]).forEach { app ->
                    if (sharedPreferenceManager.isAppHidden(app.applicationInfo.packageName, i)) {
                        allApps.add(Triple(app, launcherApps.profiles[i], i))
                    }
                }
            }

            //Sort apps by name
            sortedApps = allApps.sortedBy {
                sharedPreferenceManager.getAppName(
                    it.first.applicationInfo.packageName,
                    it.third,
                    context.packageManager.getApplicationLabel(it.first.applicationInfo)
                ).toString().lowercase()
            }
        }
        return sortedApps
    }

    fun getAppInfo(
        packageName: String,
        profile: Int
    ): ApplicationInfo? {
        return try {
            launcherApps.getApplicationInfo(packageName, 0, launcherApps.profiles[profile])
        } catch (_: Exception) {
            null
        }
    }

    fun launchApp(appInfo: LauncherActivityInfo, userHandle: UserHandle) {
        val mainActivity = launcherApps.getActivityList(appInfo.applicationInfo.packageName, userHandle).firstOrNull()
        if (mainActivity != null) {
            launcherApps.startMainActivity(mainActivity.componentName, userHandle, null, null)
        } else {
            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
        }
    }
}