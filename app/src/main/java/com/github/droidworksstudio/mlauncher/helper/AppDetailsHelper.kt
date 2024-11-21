package com.github.droidworksstudio.mlauncher.helper

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

    fun getUsageStats(context: Context, packageName: String): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val usageStatsList =
            usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        var totalUsageTime: Long = 0

        usageStatsList?.let { statsList ->
            for (usageStats in statsList) {
                if (usageStats.packageName == packageName) {
                    totalUsageTime = usageStats.totalTimeInForeground
                }
            }
        }

        return totalUsageTime
    }

    fun getTotalScreenTime(context: Context): Long {
        // Get the current time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)  // Set to the start of the day
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Define the start and end time range for stats
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        // Get the UsageStatsManager system service
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Query usage stats for the given time range
        val usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)

        if (usageStatsList.isEmpty()) {
            Log.w("getTotalScreenTime", "No usage stats available.")
            return 0L
        }

        // Sort the stats based on last used time in descending order
        usageStatsList.sortWith(compareByDescending { it.lastTimeUsed })

        // Calculate the total screen time for all apps (excluding the current app)
        var totalScreenTime: Long = 0
        val packageName = context.packageName

        for (usageStats in usageStatsList) {
            if (usageStats.packageName != packageName) {
                if (usageStats.totalTimeInForeground.toInt() != 0) {
                    Log.d(
                        "usageStatsList",
                        "App: ${usageStats.packageName}, Foreground time: ${usageStats.totalTimeInForeground}"
                    )
                    totalScreenTime += usageStats.totalTimeInForeground
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
