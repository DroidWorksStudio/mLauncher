package com.github.droidworksstudio.mlauncher.helper

//noinspection SuspiciousImport
import android.R
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log.d
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ContextThemeWrapper
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

sealed class Duration
object LongTime : Duration()
object ShortTime : Duration()

fun showToast(context: Context, message: String, duration: Duration) {
    when (duration) {
        is LongTime -> Toast.LENGTH_LONG
        is ShortTime -> Toast.LENGTH_SHORT
    }
        .let { Toast.makeText(context.applicationContext, message, it) }
        .also { it.setGravity(Gravity.CENTER, 0, 0) }
        .show()
}

fun showToastLong(context: Context, message: String) {
    showToast(context, message, LongTime)
}

fun showToastShort(context: Context, message: String) {
    showToast(context, message, ShortTime)
}

fun hasUsagePermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun showPermissionDialog(context: Context) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Permission Required")
    builder.setMessage("To continue, please grant permission to access usage data.")
    builder.setPositiveButton("Go to Settings") { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
        requestUsagePermission(context)
    }
    builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
    }
    val dialog = builder.create()
    dialog.show()
}

fun requestUsagePermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    context.startActivity(intent)
}

suspend fun getAppsList(
    context: Context,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
    includeRecentApps: Boolean = true,
): MutableList<AppListItem> {
    return withContext(Dispatchers.Main) {
        val appList: MutableList<AppListItem> = mutableListOf()
        val appRecentList: MutableList<AppListItem> = mutableListOf()
        val combinedList: MutableList<AppListItem> = mutableListOf()

        try {
            val hiddenApps = Prefs(context).hiddenApps

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            val prefs = Prefs(context)

            for (profile in userManager.userProfiles) {
                for (activity in launcherApps.getActivityList(null, profile)) {

                    // we have changed the alias identifier from app.label to app.applicationInfo.packageName
                    // therefore, we check if the old one is set if the new one is empty
                    val appAlias = prefs.getAppAlias(activity.applicationInfo.packageName).ifEmpty {
                        prefs.getAppAlias(activity.label.toString())
                    }

                    val priorities = listOf(-1.0, 0.0, 1.0, 2.0, 3.0)

                    val app = AppListItem(
                        activity.label.toString(),
                        activity.applicationInfo.packageName,
                        activity.componentName.className,
                        profile,
                        appAlias,
                        priority = priorities.random(),
                    )

                    // if the current app is not mLauncher
                    if (activity.applicationInfo.packageName != BuildConfig.APPLICATION_ID) {
                        // is this a hidden app?
                        if (hiddenApps.contains(activity.applicationInfo.packageName + "|" + profile.toString())) {
                            if (includeHiddenApps) {
                                appList.add(app)
                            }
                        } else {
                            // this is a regular app
                            if (includeRegularApps) {
                                appList.add(app)
                            }
                        }
                    }

                }

                appList.sort()

                if (prefs.recentAppsDisplayed) {
                    val appUsageTracker = AppUsageTracker.createInstance(context)
                    val lastTenUsedApps = appUsageTracker.getLastTenAppsUsed(context)

                    for ((packageName, appName, appActivityName) in lastTenUsedApps) {
                        val appAlias = prefs.getAppAlias(packageName).ifEmpty {
                            appName
                        }

                        val app = AppListItem(
                            appName,
                            packageName,
                            appActivityName,
                            profile,
                            appAlias,
                            priority = 0.0, // TODO real priority
                        )

                        d("appModel",app.toString())

                        if (includeRecentApps) {
                            appRecentList.add(app)
                            // Remove appModel from appList if its packageName matches
                            val iterator = appList.iterator()
                            while (iterator.hasNext()) {
                                val model = iterator.next()
                                if (model.appPackage == packageName) {
                                    iterator.remove()
                                }
                            }
                        }
                    }
                    // Add all elements from appRecentList
                    combinedList.addAll(appRecentList)
                }
            }
            // Add all elements from appList
            combinedList.addAll(appList)
        } catch (e: java.lang.Exception) {
            d("appList", e.toString())
        }
        combinedList
    }
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return Process.myUserHandle()
}

fun ismlauncherDefault(context: Context): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(context)
    return BuildConfig.APPLICATION_ID == launcherPackageName
}

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)
    val packageManager = context.packageManager

    val result = packageManager.resolveActivity(intent, 0)
    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else "android"
}

fun resetDefaultLauncher(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase()
    when (manufacturer) {
        "google", "essential" -> runningStockAndroid(context)
        else -> notRunningStockAndroid(context)
    }
}

private fun runningStockAndroid(context: Context) {
    try {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, FakeHomeActivity::class.java)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(selector)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun notRunningStockAndroid(context: Context) {
    try {
        val intent = Intent("android.settings.HOME_SETTINGS")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback to general settings if specific launcher settings are not found
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        launcher.startAppDetailsActivity(intent.component, userHandle, null, null)
    } ?: showToastShort(context, "Unable to to open app info")
}

fun openDialerApp(context: Context) {
    try {
        val sendIntent = Intent(Intent.ACTION_DIAL)
        context.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {
        d("openDialerApp", e.toString())
    }
}

fun openCameraApp(context: Context) {
    try {
        val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        context.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {
        d("openCameraApp", e.toString())
    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        context.startActivity(intent)
    } catch (e: java.lang.Exception) {
        d("openAlarmApp", e.toString())
    }
}

fun openDigitalWellbeing(context: Context) {
    val packageName = "com.google.android.apps.wellbeing"
    val className = "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"

    val intent = Intent()
    intent.component = ComponentName(packageName, className)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Digital Wellbeing app is not installed or cannot be opened
        // Handle this case as needed
        showToastLong(context,"Digital Wellbeing is not available on this device.")
    }
}

fun openCalendar(context: Context) {
    try {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val time = cal.time.time
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(time.toString())
        context.startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            context.startActivity(intent)
        } catch (e: Exception) {
            d("openCalendar", e.toString())
        }
    }
}

fun isTablet(context: Context): Boolean {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getMetrics(metrics)
    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
    if (diagonalInches >= 7.0) return true
    return false
}

fun initActionService(context: Context): ActionService? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val actionService = ActionService.instance()
        if (actionService != null) {
            return actionService
        } else {
            openAccessibilitySettings(context)
        }
    } else {
        showToastLong(context, "This action requires Android P (9) or higher")
    }

    return null
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val cs = ComponentName(context.packageName, ActionService::class.java.name).flattenToString()
    val bundle = Bundle()
    bundle.putString(":settings:fragment_args_key", cs)
    intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", bundle)
    }
    context.startActivity(intent)
}

fun showStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())
    else
        @Suppress("DEPRECATION", "InlinedApi")
        activity.window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
}

fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun dp2px(resources: Resources, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun storeFile(activity: Activity) {
    // Generate a unique filename with a timestamp
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "backup_$timeStamp.json"

    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, fileName)
    }
    activity.startActivityForResult(intent, Constants.BACKUP_WRITE, null)
}

fun loadFile(activity: Activity) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
    }
    activity.startActivityForResult(intent, Constants.BACKUP_READ, null)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getHexForOpacity(context: Context, prefs: Prefs): Int {
    val setColor = prefs.opacityNum

    val backgroundColor = getBackgroundColor(context)
    val hexAccentColor = java.lang.String.format("%06X", 0xFFFFFF and backgroundColor)

    var hex = Integer.toHexString(setColor).toString()
    if (hex.length < 2)
        hex = "$hex$hex"

    return android.graphics.Color.parseColor("#${hex}$hexAccentColor")
}

@RequiresApi(Build.VERSION_CODES.Q)
fun getHexFontColor(context: Context, prefs: Prefs): Int {
    return if (prefs.followAccentColors) {
        val accentColor = getAccentColor(context)
        val hexAccentColor = java.lang.String.format("#%06X", 0xFFFFFF and accentColor)

        android.graphics.Color.parseColor(hexAccentColor)
    } else {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        typedValue.data
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getBackgroundColor(context: Context): Int {
    val typedValue = TypedValue()
    val contextThemeWrapper = ContextThemeWrapper(
        context,
        R.style.Theme_DeviceDefault_DayNight
    )
    contextThemeWrapper.theme.resolveAttribute(
        R.attr.windowBackground,
        typedValue, true
    )
    return typedValue.data
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getAccentColor(context: Context): Int {
    val typedValue = TypedValue()
    val contextThemeWrapper = ContextThemeWrapper(
        context,
        R.style.Theme_DeviceDefault_DayNight
    )
    contextThemeWrapper.theme.resolveAttribute(
        R.attr.colorAccent,
        typedValue, true
    )
    return typedValue.data
}