package com.github.droidworksstudio.mlauncher.helper

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.github.droidworksstudio.mlauncher.R
import org.xmlpull.v1.XmlPullParser

object AppDetailsHelper {

    fun Context.isSystemApp(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                    || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0))
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("NewApi")
    fun getUsageStats(context: Context, packageName: String): Long {
        val blacklist = parseBlacklistXML(context)
        val pm = context.packageManager

        // Get current time in milliseconds (epoch time)
        val currentTime = System.currentTimeMillis()

        // Calculate start of the day (midnight) in epoch time
        val startTime = currentTime - (currentTime % (24 * 60 * 60 * 1000)) // Set the time to midnight of the current day

        val endTime = System.currentTimeMillis()

        // Get UsageStatsManager system service
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L // Return 0 if service is unavailable

        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            ?: return 0L // Return 0 if no data is available

        var totalUsageTime: Long = 0

        for (usageStats in usageStatsList) {
            val packageNameToCheck = usageStats.packageName

            // Skip if not the target package, if blacklisted, or if there's no recorded usage
            if (packageNameToCheck != packageName ||
                blacklist.contains(packageNameToCheck) ||
                usageStats.totalTimeVisible <= 0
            ) {
                continue
            }

            // Exclude system services (apps without a launcher activity)
            if (pm.getLaunchIntentForPackage(packageNameToCheck) == null) {
                continue
            }

            totalUsageTime += usageStats.totalTimeVisible
        }
        return totalUsageTime
    }


    @SuppressLint("NewApi")
    fun getTotalScreenTime(context: Context): Long {
        val blacklist = parseBlacklistXML(context)

        // Get current time in milliseconds (epoch time)
        val currentTime = System.currentTimeMillis()

        // Calculate start of the day (midnight) in epoch time
        val startTime = currentTime - (currentTime % (24 * 60 * 60 * 1000)) // Set the time to midnight of the current day

        val endTime = System.currentTimeMillis()

        // Get the UsageStatsManager system service
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Query usage stats for today (from midnight to current time)
        val usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        if (usageStatsList.isEmpty()) {
            Log.w("getTotalScreenTime", "No usage stats available.")
            return 0L
        }

        // Calculate the total screen time for all apps (excluding the current app)
        val pm = context.packageManager
        val packageName = context.packageName
        var totalScreenTime: Long = 0

        for (usageStats in usageStatsList) {
            val packageNameToCheck = usageStats.packageName

            // Skip current app, blacklisted apps, and apps without usage time
            if (packageNameToCheck == packageName ||
                blacklist.contains(packageNameToCheck) ||
                usageStats.totalTimeVisible <= 0
            ) continue

            // Exclude system services (apps without a launcher activity)
            if (pm.getLaunchIntentForPackage(packageNameToCheck) == null) {
                continue
            }

            totalScreenTime += usageStats.totalTimeVisible
        }

        return totalScreenTime
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

    fun formatMillisToHMS(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)

        val formattedString = StringBuilder()
        if (hours > 0) {
            formattedString.append("$hours h ")
        }
        if (minutes > 0 || hours > 0) {
            formattedString.append("$minutes m ")
        }
        formattedString.append("")
        return formattedString.toString().trim()
    }
}
