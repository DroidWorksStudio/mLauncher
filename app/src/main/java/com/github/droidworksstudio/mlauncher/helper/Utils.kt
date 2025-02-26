package com.github.droidworksstudio.mlauncher.helper

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.UiModeManager
import android.app.role.RoleManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.Log
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
import java.io.File
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
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            val prefs = Prefs(context)

            for (profile in userManager.userProfiles) {

                // Check if the profile is private space
                val isProfilePrivate = isPrivateSpaceProfile(context, profile)

                // Skip the private space if it's locked and we don't want to include private space apps
                if (isProfilePrivate && isPrivateSpaceLocked(context)) {
                    continue
                }

                for (activity in launcherApps.getActivityList(null, profile)) {

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

                    // Filter out mLauncher
                    if (activity.applicationInfo.packageName != BuildConfig.APPLICATION_ID) {
                        // Check if it's a hidden app
                        if (hiddenApps.contains(activity.applicationInfo.packageName + "|" + profile.toString())) {
                            if (includeHiddenApps) {
                                appList.add(app)
                            }
                        } else {
                            // Regular app
                            if (includeRegularApps) {
                                appList.add(app)
                            }
                        }
                    }

                }

                appList.sort()

                // Handle recent apps
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
                        )

                        if (includeRecentApps) {
                            appRecentList.add(app)

                            // Remove the app from appList if its packageName matches
                            val iterator = appList.iterator()
                            while (iterator.hasNext()) {
                                val model = iterator.next()
                                if (model.activityPackage == packageName) {
                                    iterator.remove()
                                }
                            }
                        }
                    }
                    // Add all recent apps to the combined list
                    combinedList.addAll(appRecentList)
                }
            }
            // Add regular apps to the combined list
            combinedList.addAll(appList)
        } catch (e: Exception) {
            Log.d("appList", e.toString())
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    } else {
        val testIntent = Intent(Intent.ACTION_MAIN)
        testIntent.addCategory(Intent.CATEGORY_HOME)
        val defaultHome = testIntent.resolveActivity(context.packageManager)?.packageName
        return defaultHome == context.packageName
    }
}

fun setDefaultHomeScreen(context: Context, checkDefault: Boolean = false) {
    val isDefault = ismlauncherDefault(context)
    if (checkDefault && isDefault) {
        // Launcher is already the default home app
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && context is Activity
        && !isDefault // using role manager only works when µLauncher is not already the default.
    ) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        context.startActivityForResult(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
            Constants.REQUEST_SET_DEFAULT_HOME
        )
        return
    }

    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(intent)
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

@RequiresApi(Build.VERSION_CODES.R)
fun shareApplicationButton(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND)

    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Application")
    shareIntent.putExtra(
        Intent.EXTRA_TEXT,
        checkWhoInstalled(context)
    )
    context.startActivity(Intent.createChooser(shareIntent, "Share Application"))
}


@Suppress("DEPRECATION")
fun checkWhoInstalled(context: Context): String {
    val appName = context.getString(R.string.app_name)
    val descriptionTemplate = context.getString(R.string.advanced_settings_share_application_description)
    val descriptionTemplate2 = context.getString(R.string.advanced_settings_share_application_description_addon)

    // Get the installer package name
    val installer: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // For Android 11 (API 30) and above
        val installSourceInfo = context.packageManager.getInstallSourceInfo(context.packageName)
        installSourceInfo.installingPackageName
    } else {
        // For older versions
        context.packageManager.getInstallerPackageName(context.packageName)
    }

    // Handle null installer package name
    val installSource = when (installer) {
        "com.android.vending" -> "Google Play Store"
        "org.fdroid.fdroid" -> "F-Droid"
        null -> "GitHub" // In case installer is null
        else -> installer // Default to the installer package name
    }

    val installURL = when (installer) {
        "com.android.vending" -> "Google Play Store"
        "org.fdroid.fdroid" -> "https://f-droid.org/packages/app.mlauncher"
        null -> "https://github.com/DroidWorksStudio/mLauncher" // In case installer is null
        else -> "Google Play Store" // Default to the Google Play Store
    }

    // Format the description with the app name and install source
    return String.format(
        "%s %s",
        String.format(descriptionTemplate, appName),
        String.format(descriptionTemplate2, installSource, installURL)
    )
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

fun storeFile(activity: Activity, backupType: Constants.BackupType) {
    // Generate a unique filename with a timestamp
    when (backupType) {
        Constants.BackupType.FullSystem -> {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "backup_$timeStamp.json"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            activity.startActivityForResult(intent, Constants.BACKUP_WRITE, null)
        }

        Constants.BackupType.Theme -> {
            val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "theme_$timeStamp.mtheme"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            activity.startActivityForResult(intent, Constants.THEME_BACKUP_WRITE, null)
        }

    }

}

fun loadFile(activity: Activity, backupType: Constants.BackupType) {
    when (backupType) {
        Constants.BackupType.FullSystem -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            activity.startActivityForResult(intent, Constants.BACKUP_READ, null)
        }

        Constants.BackupType.Theme -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            activity.startActivityForResult(intent, Constants.THEME_BACKUP_READ, null)
        }
    }

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

fun getTrueSystemFont(): Typeface {
    val possibleSystemFonts = listOf(
        "/system/fonts/Roboto-Regular.ttf",      // Stock Android (Pixel, AOSP)
        "/system/fonts/NotoSans-Regular.ttf",    // Some Android One devices
        "/system/fonts/SamsungOne-Regular.ttf",  // Samsung
        "/system/fonts/MiSans-Regular.ttf",      // Xiaomi MIUI
        "/system/fonts/OPSans-Regular.ttf"       // OnePlus
    )

    for (fontPath in possibleSystemFonts) {
        val fontFile = File(fontPath)
        if (fontFile.exists()) {
            return Typeface.createFromFile(fontFile)
        }
    }

    // Fallback to Roboto as a default if no system font is found
    return Typeface.DEFAULT
}