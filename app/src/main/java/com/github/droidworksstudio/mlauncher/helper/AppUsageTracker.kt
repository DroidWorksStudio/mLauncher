package com.github.droidworksstudio.mlauncher.helper

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Prefs
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

class AppUsageTracker private constructor(context: Context) {
    // TODO looks like a singleton? Shall we just make a top-level object in the file?
    companion object {
        private var instance: AppUsageTracker? = null

        fun createInstance(context: Context): AppUsageTracker {
            if (instance == null) {
                instance = AppUsageTracker(context.applicationContext)
            }
            return instance!!
        }
    }

    private val appLastUsedMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    private val packageManager: PackageManager = context.packageManager

    fun updateLastUsedTimestamp(packageName: String) {
        val currentTime = System.currentTimeMillis()
        appLastUsedMap[packageName] = currentTime
    }

    fun getLastTenAppsUsed(context: Context): List<Triple<String, String, String>> {
        val recentApps = mutableSetOf<String>() // Set is to store unique package names
        val result = mutableListOf<Triple<String, String, String>>() // List to store recent apps

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // 24 hours ago
        val blacklist = parseBlacklistXML(context)
        val prefs = Prefs(context)

        usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)?.let { usageStatsList ->
            val sortedList = usageStatsList
                .filter { isPackageLaunchable(context, it.packageName, blacklist) }
                .sortedByDescending { it.lastTimeUsed }

            sortedList.forEach { usageStats ->
                val packageName = usageStats.packageName
                if (packageName != context.packageName && !recentApps.contains(packageName)) {
                    val appName = getAppNameFromPackage(packageName)
                    val className = getComponentNameFromPackage(context, packageName)
                    val appActivityName = className.toString()
                    Log.d("appActivityName", appActivityName)
                    if (appName != null) {
                        recentApps.add(packageName)
                        result.add(Triple(packageName, appName, appActivityName))
                    }
                }
            }
        }

        return result.take(prefs.recentCounter) // Return up to 10 recent apps
    }


    private fun isPackageLaunchable(context: Context, packageName: String, blacklist: List<String>): Boolean {
        if (isAppInBlacklist(packageName, blacklist)){
            return false
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return launchIntent != null
    }

    private fun isAppInBlacklist(appPackageName: String, blacklist: List<String>): Boolean {
        return blacklist.contains(appPackageName)
    }

    private fun parseBlacklistXML(context: Context): List<String> {
        val packageNames = mutableListOf<String>()

        // Obtain an XmlPullParser for the blacklist.xml file
        context.resources.getXml(R.xml.blacklist).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "app") {
                    val packageName = parser.getAttributeValue(null, "packageName")
                    packageNames.add(packageName)
                }
                parser.next()
            }
        }

        return packageNames
    }

    private fun getComponentNameFromPackage(context: Context, packageName: String): String? {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            val componentName = launchIntent.component
            componentName?.className
        } else {
            null
        }
    }


    private fun getAppNameFromPackage(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
