package com.github.droidworksstudio.mlauncher.helper

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.USAGE_STATS_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.github.droidworksstudio.mlauncher.R
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
    fun getUsageStats(context: Context, targetPackageName: String): Long {
        val packageManager = context.packageManager
        val blacklist = parseBlacklistXML(context)

        // Get start of today in the correct timezone
        val startOfToday = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentTime = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
        }.timeInMillis

        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L // Return 0 if service is unavailable

        // Get launcher apps (home screen apps)
        val launcherApps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.toSet()

        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfToday, currentTime)
            ?: return 0L // Return 0 if no data is available

        return usageStatsList
            .filter { usageStats ->
                val packageNameToCheck = usageStats.packageName

                // Only keep the stats of the specific package
                val isValid = packageNameToCheck == targetPackageName &&
                        !launcherApps.contains(packageNameToCheck) &&
                        !blacklist.contains(packageNameToCheck) && // Check against the blacklist
                        usageStats.totalTimeInForeground > 0 && // Ensure there's usage time
                        startOfToday <= usageStats.lastTimeUsed  // Ensure usage happened today
                // Log the valid app's stats if it's valid
                if (isValid) {
                    Log.d(
                        "getUsageStats",
                        "${usageStats.packageName} : ${formatMillisToHMS(usageStats.totalTimeInForeground, true)} " +
                                "${formatLongToCalendar(startOfToday)} ${formatLongToCalendar(usageStats.lastTimeUsed)}"
                    )
                }

                isValid
            }
            .sumOf { it.totalTimeInForeground } // Sum the total time for valid apps
    }


    @SuppressLint("NewApi")
    fun getTotalScreenTime(context: Context): Long {
        val packageManager = context.packageManager
        val blacklist = parseBlacklistXML(context)
        val currentPackageName = context.packageName // Get current app's package name

        // Get start of today in the correct timezone
        val startOfToday = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentTime = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
        }.timeInMillis
        val usageStatsManager = context.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        // Get launcher apps (home screen apps)
        val launcherApps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        ).map { it.activityInfo.packageName }.toSet()

        // Query usage stats for today
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startOfToday, currentTime
        ) ?: return 0L

        return usageStatsList
            .filter { usageStats ->
                val packageName = usageStats.packageName
                // Exclude launcher apps, blacklisted apps, and current app
                val isValid = packageName != currentPackageName &&
                        !launcherApps.contains(packageName) &&
                        !blacklist.contains(packageName) &&
                        usageStats.totalTimeInForeground > 0 &&
                        // Ensure usage happened today
                        startOfToday <= usageStats.lastTimeUsed
                if (isValid) {
                    Log.d(
                        "getTotalScreenTime",
                        "${usageStats.packageName} : ${formatMillisToHMS(usageStats.totalTimeVisible, true)} ${formatLongToCalendar(startOfToday)} ${
                            formatLongToCalendar(
                                usageStats
                                    .lastTimeUsed
                            )
                        }"
                    )
                }
                isValid
            }
            .sumOf { it.totalTimeInForeground }
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

    private fun formatLongToCalendar(longTimestamp: Long): String {
        // Create a Calendar instance and set its time to the given timestamp (in milliseconds)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = longTimestamp
        }

        // Format the calendar object to a readable string
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy, HH:mm:ss", Locale.getDefault()) // You can modify the format
        return dateFormat.format(calendar.time) // Return the formatted date string
    }

    fun formatMillisToHMS(millis: Long, showSeconds: Boolean): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000

        val formattedString = StringBuilder()
        if (hours > 0) {
            formattedString.append("$hours h ")
        }
        if (minutes > 0 || hours > 0) {
            formattedString.append("$minutes m ")
        }
        // Only append seconds if showSeconds is true
        if (showSeconds) {
            formattedString.append("$seconds s")
        }

        return formattedString.toString().trim()
    }
}
