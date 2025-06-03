package com.github.droidworksstudio.mlauncher.helper

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.UiModeManager
import android.app.role.RoleManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.withSave
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.github.droidworksstudio.common.ColorIconsExtensions
import com.github.droidworksstudio.common.CrashHandler
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.common.getLocalizedStringArray
import com.github.droidworksstudio.common.openAccessibilitySettings
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.BuildConfig
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.AppCategory
import com.github.droidworksstudio.mlauncher.data.AppListItem
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Message
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.analytics.AppUsageMonitor
import com.github.droidworksstudio.mlauncher.helper.utils.PrivateSpaceManager
import com.github.droidworksstudio.mlauncher.helper.utils.packageNames
import com.github.droidworksstudio.mlauncher.services.ActionService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.pow
import kotlin.math.sqrt

fun emptyString(): String {
    return ""
}

val iconPackActions = listOf(
    "app.mlauncher.THEME",
    "org.adw.launcher.THEMES",
    "com.gau.go.launcherex.theme",
    "com.novalauncher.THEME",
    "com.anddoes.launcher.THEME",
    "com.teslacoilsw.launcher.THEME",
    "app.lawnchair.icons.THEMED_ICON"
)

val iconPackBlacklist = listOf(
    "ginlemon.iconpackstudio"
)

fun hasUsageAccessPermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasLocationPermission(context: Context): Boolean {
    val fineLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val coarseLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    return fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED
}


fun showPermissionDialog(context: Context) {
    CrashHandler.logUserAction("Show Usage Permission Dialog")
    val builder = MaterialAlertDialogBuilder(context)
    builder.setTitle(getLocalizedString(R.string.permission_required))
    builder.setMessage(getLocalizedString(R.string.access_usage_data_permission))
    builder.setPositiveButton(getLocalizedString(R.string.goto_settings)) { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
        requestUsagePermission(context)
    }
    builder.setNegativeButton(getLocalizedString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int ->
        dialogInterface.dismiss()
    }
    val dialog = builder.create()
    dialog.show()
}

fun requestUsagePermission(context: Context) {
    CrashHandler.logUserAction("Requested Usage Permission")
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()  // Open settings for YOUR app only
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun getAppsList(
    context: Context,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
    includeRecentApps: Boolean = true
): MutableList<AppListItem> = withContext(Dispatchers.Main) {

    val fullList: MutableList<AppListItem> = mutableListOf()
    val scrollIndexMap = mutableMapOf<Char, Int>()

    Log.d(
        "AppListDebug",
        "🔄 getAppsList called with: includeRegular=$includeRegularApps, includeHidden=$includeHiddenApps, includeRecent=$includeRecentApps"
    )
    CrashHandler.logUserAction("Display App List")

    try {
        val prefs = Prefs(context)
        val hiddenApps = prefs.hiddenApps
        val pinnedPackages = prefs.pinnedApps.toSet()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val seenPackages = mutableSetOf<String>()

        for (profile in userManager.userProfiles) {
            Log.d("AppListDebug", "👤 Processing user profile: $profile")

            val isPrivate = PrivateSpaceManager(context).isPrivateSpaceProfile(profile)
            if (isPrivate && PrivateSpaceManager(context).isPrivateSpaceLocked()) {
                Log.d("AppListDebug", "🔒 Skipping locked private space for profile: $profile")
                continue
            }

            // Recent Apps
            if (prefs.recentAppsDisplayed && includeRecentApps && fullList.none { it.category == AppCategory.RECENT }) {
                val tracker = AppUsageMonitor.createInstance(context)
                val recentApps = tracker.getLastTenAppsUsed(context)

                Log.d("AppListDebug", "🕓 Adding ${recentApps.size} recent apps")

                for ((packageName, appName, activityName) in recentApps) {
                    if (seenPackages.contains(packageName)) continue
                    val alias = prefs.getAppAlias(packageName).ifEmpty { appName }

                    fullList.add(
                        AppListItem(appName, packageName, activityName, profile, alias, AppCategory.RECENT)
                    )
                    seenPackages.add(packageName)
                }
            }

            // Launcher Apps
            val launcherAppList = launcherApps.getActivityList(null, profile)
            Log.d("AppListDebug", "📦 Found ${launcherAppList.size} launcher apps for profile: $profile")

            for (activity in launcherAppList) {
                val packageName = activity.applicationInfo.packageName
                val className = activity.componentName.className
                val label = activity.label.toString()

                if (packageName == BuildConfig.APPLICATION_ID) continue
                if (seenPackages.contains(packageName)) continue

                val isHidden = hiddenApps.contains("$packageName|$profile")
                if ((isHidden && !includeHiddenApps) || (!isHidden && !includeRegularApps)) continue

                val alias = prefs.getAppAlias(packageName).ifEmpty {
                    prefs.getAppAlias(label)
                }

                val category = when {
                    pinnedPackages.contains(packageName) -> AppCategory.PINNED
                    else -> AppCategory.REGULAR
                }

                fullList.add(
                    AppListItem(label, packageName, className, profile, alias, category)
                )

                seenPackages.add(packageName)
            }
        }

        // Sort the list: Pinned → Regular → Recent; then alphabetical within category
        fullList.sortWith(
            compareBy<AppListItem> { it.category.ordinal }
                .thenBy { it.label.lowercase() }
        )

        // Build scroll index (excluding pinned apps)
        for ((index, item) in fullList.withIndex()) {
            if (item.category == AppCategory.PINNED) continue
            val firstChar = item.label.firstOrNull()?.uppercaseChar() ?: continue
            scrollIndexMap.putIfAbsent(firstChar, index)
        }

        Log.d("AppListDebug", "✅ App list built with ${fullList.size} items")

    } catch (e: Exception) {
        Log.e("AppListDebug", "❌ Error building app list: ${e.message}", e)
    }

    fullList
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

fun getNextAlarm(context: Context, prefs: Prefs): CharSequence {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val is24HourFormat = DateFormat.is24HourFormat(context)
    val nextAlarmClock = alarmManager.nextAlarmClock ?: return "No alarm is set."

    val alarmTime = nextAlarmClock.triggerTime
    val timezone =
        prefs.appLanguage.timezone()  // Assuming this returns a string like "America/New_York"
    val formattedDate = DateFormat.getBestDateTimePattern(timezone, "eeeddMMM")
    val best12 = DateFormat.getBestDateTimePattern(
        timezone,
        if (prefs.showClockFormat) "hhmma" else "hhmm"
    ).let {
        if (!prefs.showClockFormat) it.removeSuffix(" a") else it
    }
    val best24 = DateFormat.getBestDateTimePattern(timezone, "HHmm")
    val formattedTime = if (is24HourFormat) best24 else best12
    val formattedAlarm =
        SimpleDateFormat("$formattedDate $formattedTime", Locale.getDefault()).format(alarmTime)

    val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_alarm_clock)
    val fontSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        (prefs.alarmSize / 1.5).toFloat(),
        context.resources.displayMetrics
    ).toInt()

    drawable?.apply {
        setBounds(0, 0, fontSize, fontSize)
        val colorFilterColor: ColorFilter =
            PorterDuffColorFilter(prefs.alarmClockColor, PorterDuff.Mode.SRC_IN)
        drawable.colorFilter = colorFilterColor
    }

    val imageSpan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ImageSpan(drawable!!, ImageSpan.ALIGN_CENTER)
    } else {
        CenteredImageSpan(drawable!!)
    }

    return SpannableStringBuilder(" ").apply {
        setSpan(
            imageSpan,
            0, 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        append(" $formattedAlarm")
    }
}

fun wordOfTheDay(prefs: Prefs): String {
    val dailyWordsArray = loadWordList(prefs)
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
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        return context.packageName == defaultLauncherPackage
    }
}

fun helpFeedbackButton(context: Context) {
    val uri = "https://github.com/DroidWorksStudio/mLauncher-Support".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

fun communitySupportButton(context: Context) {
    val uri = "https://discord.com/invite/qG6hFuAzfu/".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

fun checkWhoInstalled(context: Context): String {
    val appName = getLocalizedString(R.string.app_name)
    val descriptionTemplate =
        getLocalizedString(R.string.advanced_settings_share_application_description)
    val descriptionTemplate2 =
        getLocalizedString(R.string.advanced_settings_share_application_description_addon)

    val installerPackageName: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // For API level 30 and above
        val installSourceInfo = context.packageManager.getInstallSourceInfo(context.packageName)
        installSourceInfo.installingPackageName
    } else {
        // For below API level 30
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }

    // Handle null installer package name
    val installSource = when (installerPackageName) {
        "com.android.vending" -> "Google Play Store"
        else -> installerPackageName // Default to the installer package name
    }

    val installURL = when (installerPackageName) {
        "com.android.vending" -> "https://play.google.com/store/apps/details?id=app.mlauncher"
        else -> "https://play.google.com/store/apps/details?id=app.mlauncher" // Default to the Google Play Store
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
    val actionService = ActionService.instance()
    if (actionService != null) {
        return actionService
    } else {
        context.openAccessibilitySettings()
    }

    return null
}

fun hideStatusBar(activity: Activity) {
    // Ensure that the content draws behind the system bars
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)

    val insetsController = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.statusBars())
}

fun showStatusBar(activity: Activity) {
    // Restore the default behavior where content does not draw behind system bars
    WindowCompat.setDecorFitsSystemWindows(activity.window, true)

    val insetsController = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
    insetsController.show(WindowInsetsCompat.Type.statusBars())
}

fun dp2px(resources: Resources, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun sp2px(resources: Resources, sp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        resources.displayMetrics
    )
}


fun loadWordList(prefs: Prefs): List<String> {
    val customWordListString = prefs.wordList
    // If the user has imported their own list, use it
    return if (customWordListString != emptyString()) {
        prefs.wordList.split("||")
    } else {
        getLocalizedStringArray(R.array.word_of_the_day).toList()
    }
}


fun getHexForOpacity(prefs: Prefs): Int {
    // Convert the opacity percentage (0-100) to a reversed decimal (0.0-1.0)
    val setOpacity = ((100 - prefs.opacityNum.coerceIn(
        0,
        100
    )) / 100.0).toFloat() // Reverse the opacity, (0% = full opacity, 100% = transparent)

    val backgroundColor = prefs.backgroundColor // This is already an Int

    // Extract RGB from background color
    val red = android.graphics.Color.red(backgroundColor)
    val green = android.graphics.Color.green(backgroundColor)
    val blue = android.graphics.Color.blue(backgroundColor)

    // Combine opacity with RGB and return final color
    return if (prefs.showBackground) {
        // Apply a minimum opacity constant for the background
        android.graphics.Color.argb((Constants.MIN_OPACITY * 255), red, green, blue)
    } else {
        // Use the reversed opacity as a percentage (0-100%) converted to a float (0.0-1.0)
        android.graphics.Color.argb((setOpacity * 255).toInt(), red, green, blue)
    }
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

fun parseBlacklistXML(context: Context): List<String> {
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

fun sortMessages(messages: List<Message>): List<Message> {
    return messages.sortedWith(
        compareBy<Message> {
            when (it.priority) {
                "High" -> 0
                "Medium" -> 1
                "Low" -> 2
                else -> 3
            }
        }.thenByDescending { it.timestamp }
    )
}


fun formatLongToCalendar(longTimestamp: Long): String {
    // Create a Calendar instance and set its time to the given timestamp (in milliseconds)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = longTimestamp
    }

    // Format the calendar object to a readable string
    val dateFormat = SimpleDateFormat(
        "MMMM dd, yyyy, HH:mm:ss",
        Locale.getDefault()
    ) // You can modify the format
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


fun logActivitiesFromPackage(context: Context, packageName: String) {
    val packageManager = context.packageManager

    try {
        val packageInfo: PackageInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES
        )

        val activities: Array<ActivityInfo>? = packageInfo.activities

        activities?.forEach { activityInfo ->
            val componentInfoString =
                "ComponentInfo{${activityInfo.packageName}/${activityInfo.name}}"
            Log.d("ComponentInfoLog", componentInfoString)
        } ?: Log.d("ComponentInfoLog", "No activities found in package $packageName")

    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("ComponentInfoLog", "Package not found: $packageName", e)
    }
}

fun getDeviceInfo(context: Context): String {
    return try {
        val packageManager = context.packageManager
        val installSource = getInstallSource(packageManager, context.packageName)

        """
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Brand: ${Build.BRAND}
            Device: ${Build.DEVICE}
            Product: ${Build.PRODUCT}
            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            ABI: ${Build.SUPPORTED_ABIS.joinToString()}
            App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            Locale: ${Locale.getDefault()}
            Timezone: ${TimeZone.getDefault().id}
            Installed From: $installSource
            """.trimIndent()
    } catch (e: Exception) {
        "Device Info Unavailable: ${e.message}"
    }
}

private fun getInstallSource(packageManager: PackageManager, packageName: String): String {
    try {
        if (BuildConfig.DEBUG) return "Android Studio (ADB)"

        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API level 30 and above
            packageManager.getInstallSourceInfo(packageName).installingPackageName ?: "Unknown"
        } else {
            // For below API level 30
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName) ?: "Unknown"
        }

        return when (installer) {
            "com.android.vending" -> "Google Play Store"
            "org.fdroid.fdroid" -> "F-Droid"
            "com.obtanium.app" -> "Obtanium"
            "com.android.shell" -> "Android Studio (ADB)"

            // Popular browsers
            "com.android.chrome" -> "Chrome"
            "org.mozilla.firefox" -> "Firefox"
            "com.brave.browser" -> "Brave"
            "com.microsoft.emmx" -> "Edge"
            "com.opera.browser" -> "Opera"
            "com.sec.android.app.sbrowser" -> "Samsung Internet"

            // Popular file managers
            "com.google.android.documentsui" -> "Files by Google"
            "com.samsung.android.myfiles" -> "Samsung My Files"
            "com.mi.android.globalFileexplorer" -> "Xiaomi File Manager"
            "com.asus.filemanager" -> "ASUS File Manager"
            "com.lonelycatgames.Xplore" -> "X-plore File Manager"
            "nextapp.fx" -> "FX File Explorer"
            "com.amazon.filemanager" -> "Amazon File Manager"

            else -> "Unknown"
        }
    } catch (_: Exception) {
        return "Unknown"
    }
}


fun getSystemIcons(
    context: Context,
    prefs: Prefs,
    target: IconCacheTarget,
    nonNullDrawable: Drawable
): Drawable? {
    return when (target) {
        IconCacheTarget.APP_LIST -> getAppListIcons(context, prefs, nonNullDrawable)
        IconCacheTarget.HOME -> getHomeIcons(context, prefs, nonNullDrawable)
    }
}

private fun getAppListIcons(context: Context, prefs: Prefs, nonNullDrawable: Drawable): Drawable? {
    return when (prefs.iconPackAppList) {
        Constants.IconPacks.CloudDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.cloud_dots_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.LauncherDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.launcher_dot_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.NiagaraDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.niagara_dot_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.SpinnerDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.spinner_dots_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        else -> {
            null
        }
    }
}

private fun getHomeIcons(context: Context, prefs: Prefs, nonNullDrawable: Drawable): Drawable? {
    return when (prefs.iconPackHome) {
        Constants.IconPacks.CloudDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.cloud_dots_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.LauncherDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.launcher_dot_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.NiagaraDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.niagara_dot_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        Constants.IconPacks.SpinnerDots -> {
            val newIcon = ContextCompat.getDrawable(
                context,
                R.drawable.spinner_dots_icon
            )!!
            val bitmap = ColorIconsExtensions.drawableToBitmap(nonNullDrawable)
            val dominantColor = ColorIconsExtensions.getDominantColor(bitmap)
            ColorIconsExtensions.recolorDrawable(newIcon, dominantColor)
        }

        else -> {
            null
        }
    }
}

fun setTopPadding(view: View) {
    // Store the original top padding to avoid accumulating insets
    val initialTopPadding = view.paddingTop

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        // Retrieve the top inset, which includes the status bar and display cutout
        val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top

        // Apply the combined padding
        v.updatePadding(top = initialTopPadding + topInset)

        // Return the insets to allow further propagation if needed
        insets
    }

    // Request the insets to be applied
    ViewCompat.requestApplyInsets(view)
}

class CenteredImageSpan(drawable: Drawable) : ImageSpan(drawable) {
    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val drawable = drawable
        canvas.withSave {
            val transY = top + ((bottom - top) - drawable.bounds.height()) / 2
            translate(x, transY.toFloat())
            drawable.draw(this)
        }
    }
}