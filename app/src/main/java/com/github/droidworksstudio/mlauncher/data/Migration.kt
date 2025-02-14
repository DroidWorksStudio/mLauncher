package com.github.droidworksstudio.mlauncher.data

import android.content.Context
import com.github.droidworksstudio.mlauncher.BuildConfig

class Migration(val context: Context) {
    fun migratePreferencesOnVersionUpdate(prefs: Prefs) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = prefs.appVersion

        // Define a map of version code -> preferences to clear
        val versionCleanupMap = mapOf(
            171 to listOf(
                "APP_DARK_COLORS",
                "APP_LIGHT_COLORS",
                "HOME_FOLLOW_ACCENT",
                "ALL_APPS_TEXT"
            ),
            172 to listOf(
                "TIME_ALIGNMENT",
                "SHOW_TIME",
                "SHOW_TIME_FORMAT",
                "TIME_COLOR"
            ),
            // Add more versions and preferences to remove here
        )

        // Iterate over the versions and clear the relevant preferences
        for ((version, keys) in versionCleanupMap) {
            // Only clear preferences for versions between savedVersionCode and currentVersionCode
            if (version in (savedVersionCode + 1)..currentVersionCode) {
                // Remove the preferences for this version
                keys.forEach { key ->
                    prefs.remove(key)
                }
            }
        }

        // Update the stored version code after cleanup
        prefs.appVersion = currentVersionCode
    }
}