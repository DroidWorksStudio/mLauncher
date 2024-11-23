package com.github.droidworksstudio.mlauncher.helper

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.util.Calendar

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
        // Set calendar to midnight of today (start of the day)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis // Midnight today
        val endTime = System.currentTimeMillis() // Current time

        // Get UsageStatsManager system service
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        // Query usage stats for the specific time range (startTime to endTime)
        val usageStatsList = usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        var totalUsageTime: Long = 0

        // Iterate through the stats to get the specific package usage
        usageStatsList?.let { statsList ->
            for (usageStats in statsList) {
                if (usageStats.packageName == packageName) {
                    // Use totalTimeInForeground for actual usage time
                    if (usageStats.totalTimeVisible > 0) {
                        totalUsageTime += usageStats.totalTimeVisible
                    }
                }
            }
        }

        return totalUsageTime
    }


    @SuppressLint("NewApi")
    fun getTotalScreenTime(context: Context): Long {
        // Get the start of the current day (midnight)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
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
        var totalScreenTime: Long = 0
        val packageName = context.packageName

        for (usageStats in usageStatsList) {
            if (usageStats.packageName != packageName) {
                // Only consider apps with non-zero foreground time
                if (usageStats.totalTimeVisible > 0) {
                    totalScreenTime += usageStats.totalTimeVisible
                }
            }
        }

        return totalScreenTime
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
