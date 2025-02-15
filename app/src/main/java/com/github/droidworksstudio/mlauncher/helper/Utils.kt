package com.github.droidworksstudio.mlauncher.helper

//noinspection SuspiciousImport
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.Log.d
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import com.github.droidworksstudio.common.openAccessibilitySettings
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.R
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
                    // TODO inline this fallback logic to prefs
                    val appAlias = prefs.getAppAlias(activity.applicationInfo.packageName).ifEmpty {
                        prefs.getAppAlias(activity.label.toString())
                    }

                    val app = AppListItem(
                        activity.label.toString(),
                        activity.applicationInfo.packageName,
                        activity.componentName.className,
                        user = profile,
                        appAlias,
                    )

                    // TODO rewrite as a filter
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
                            // recent apps are sorted by last usage time
                        )

                        d("appModel", app.toString())

                        if (includeRecentApps) {
                            appRecentList.add(app)
                            // Remove appModel from appList if its packageName matches
                            val iterator = appList.iterator()

                            // FIXME likely a performance issue (a cycle inside a cycle)
                            //       when I enable "recent apps", the drawer opens significantly slower.
                            while (iterator.hasNext()) {
                                val model = iterator.next()
                                if (model.activityPackage == packageName) {
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

@RequiresApi(Build.VERSION_CODES.Q)
fun getNextAlarm(context: Context, prefs: Prefs): CharSequence {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val nextAlarmClock = alarmManager.nextAlarmClock ?: return "No alarm is set."

    val alarmTime = nextAlarmClock.triggerTime
    val formattedTime = SimpleDateFormat("EEE, MMM d hh:mm a", Locale.getDefault()).format(alarmTime)

    val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_alarm_clock)
    val fontSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        (prefs.alarmSize / 1.5).toFloat(),
        context.resources.displayMetrics
    ).toInt()

    drawable?.apply {
        setBounds(0, 0, fontSize, fontSize)
        val colorFilterColor: ColorFilter = PorterDuffColorFilter(prefs.alarmClockColor, PorterDuff.Mode.SRC_IN)
        drawable.colorFilter = colorFilterColor
    }

    return SpannableStringBuilder(" ").apply {
        drawable?.let {
            setSpan(
                ImageSpan(it, ImageSpan.ALIGN_CENTER),
                0, 1,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        append(" $formattedTime")
    }
}

fun wordOfTheDay(resources: Resources): String {
    val dailyWordsArray =
        resources.getStringArray(R.array.word_of_the_day)
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val wordIndex =
        (dayOfYear - 1) % dailyWordsArray.size // Subtracting 1 to align with array indexing
    return dailyWordsArray[wordIndex]
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
    try {
        val intent = Intent("android.settings.HOME_SETTINGS")
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Fallback to general settings if specific launcher settings are not found
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun helpFeedbackButton(context: Context) {
    val uri = Uri.parse("https://github.com/DroidWorksStudio/mLauncher")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

fun communitySupportButton(context: Context) {
    val uri = Uri.parse("https://discord.gg/qG6hFuAzfu")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

fun shareApplicationButton(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    val description = context.getString(
        R.string.advanced_settings_share_application_description,
        context.getString(R.string.app_name)
    )
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Application")
    shareIntent.putExtra(
        Intent.EXTRA_TEXT,
        "$description https://f-droid.org/packages/${context.packageName}"
    )
    context.startActivity(Intent.createChooser(shareIntent, "Share Application"))
}

fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        launcher.startAppDetailsActivity(intent.component, userHandle, null, null)
    } ?: context.showLongToast("Unable to to open app info")
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
            context.openAccessibilitySettings()
        }
    } else {
        context.showLongToast("This action requires Android P (9) or higher")
    }

    return null
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


fun getHexForOpacity(prefs: Prefs): Int {
    val setOpacity = prefs.opacityNum.coerceIn(0, 255) // Ensure opacity is in the range (0-255)
    val backgroundColor = prefs.backgroundColor // This is already an Int

    // Extract RGB from background color
    val red = android.graphics.Color.red(backgroundColor)
    val green = android.graphics.Color.green(backgroundColor)
    val blue = android.graphics.Color.blue(backgroundColor)

    // Combine opacity with RGB and return final color
    return android.graphics.Color.argb(setOpacity, red, green, blue)
}

fun isSystemInDarkMode(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
}

fun setThemeMode(context: Context, isDark: Boolean, view: View) {
    // Retrieve background color based on the theme
    val backgroundAttr = if (isDark) R.attr.backgroundDark else R.attr.backgroundLight

    val typedValue = TypedValue()
    val theme: Resources.Theme = context.theme
    theme.resolveAttribute(backgroundAttr, typedValue, true)

    // Apply the background color from styles.xml
    view.setBackgroundResource(typedValue.resourceId)
}