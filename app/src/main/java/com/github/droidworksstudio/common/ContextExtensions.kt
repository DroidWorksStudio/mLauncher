package com.github.droidworksstudio.common

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.helper.ActionService
import java.util.Calendar
import java.util.Date

fun Context.inflate(resource: Int, root: ViewGroup? = null, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(this).inflate(resource, root, attachToRoot)
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.showShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}


fun Context.openUrl(url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun isGestureNavigationEnabled(context: Context): Boolean {
    return try {
        val navigationMode = Settings.Secure.getInt(
            context.contentResolver,
            "navigation_mode"  // This is the key to check the current navigation mode
        )
        // 2 corresponds to gesture navigation mode
        navigationMode == 2
    } catch (e: Settings.SettingNotFoundException) {
        // Handle the case where the setting isn't found, assume not enabled
        false
    }
}

fun Context.launchCalendar() {
    try {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val time = cal.time.time
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(time.toString())
        this.startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            this.startActivity(intent)
        } catch (e: Exception) {
            d("openCalendar", e.toString())
        }
    }
}

fun Context.openDialerApp() {
    try {
        val sendIntent = Intent(Intent.ACTION_DIAL)
        this.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {
        d("openDialerApp", e.toString())
    }
}

fun Context.openAlarmApp() {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        this.startActivity(intent)
    } catch (e: java.lang.Exception) {
        d("openAlarmApp", e.toString())
    }
}

fun Context.openCameraApp() {
    try {
        val sendIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        this.startActivity(sendIntent)
    } catch (e: java.lang.Exception) {
        d("openCameraApp", e.toString())
    }
}

fun Context.openBatteryManager() {
    try {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        this.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showLongToast("Battery manager settings are not available on this device.")
    }
}

fun Context.openDigitalWellbeing() {
    val packageName = "com.google.android.apps.wellbeing"
    val className = "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"

    val intent = Intent()
    intent.component = ComponentName(packageName, className)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    try {
        this.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Digital Wellbeing app is not installed or cannot be opened
        // Handle this case as needed
        this.showLongToast("Digital Wellbeing is not available on this device.")
    }
}

fun Context.searchOnPlayStore(query: String? = null): Boolean {
    return try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW)
        playStoreIntent.data = Uri.parse("${Constants.APP_GOOGLE_PLAY_STORE}=$query")

        // Check if the Play Store app is installed
        if (playStoreIntent.resolveActivity(packageManager) != null) {
            startActivity(playStoreIntent)
        } else {
            // If Play Store app is not installed, open Play Store website in browser
            playStoreIntent.data = Uri.parse("${Constants.URL_GOOGLE_PLAY_STORE}=$query")
            startActivity(playStoreIntent)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.searchCustomSearchEngine(searchQuery: String? = null, prefs: Prefs): Boolean {
    val searchUrl = when (prefs.searchEngines) {
        Constants.SearchEngines.Google -> {
            Constants.URL_GOOGLE_SEARCH
        }

        Constants.SearchEngines.Yahoo -> {
            Constants.URL_YAHOO_SEARCH
        }

        Constants.SearchEngines.DuckDuckGo -> {
            Constants.URL_DUCK_SEARCH
        }

        Constants.SearchEngines.Bing -> {
            Constants.URL_BING_SEARCH
        }

        Constants.SearchEngines.Brave -> {
            Constants.URL_BRAVE_SEARCH
        }

        Constants.SearchEngines.SwissCow -> {
            Constants.URL_SWISSCOW_SEARCH
        }
    }
    val encodedQuery = Uri.encode(searchQuery)
    val fullUrl = "$searchUrl$encodedQuery"
    d("fullUrl", fullUrl)
    openUrl(fullUrl)
    return true
}

fun Context.hasInternetPermission(): Boolean {
    val permission = Manifest.permission.INTERNET
    val result = ContextCompat.checkSelfPermission(this, permission)
    return result == PackageManager.PERMISSION_GRANTED
}

fun Context.isPackageInstalled(
    packageName: String,
    userHandle: UserHandle = android.os.Process.myUserHandle()
): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.isNotEmpty()
}

fun Context.getAppNameFromPackageName(packageName: String): String? {
    val packageManager = this.packageManager
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo) as String
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

fun Context.isBiometricEnabled(): Boolean {
    val biometricManager = BiometricManager.from(this)

    return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        else -> false
    }
}


fun Context.openAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val cs = ComponentName(this.packageName, ActionService::class.java.name).flattenToString()
    val bundle = Bundle()
    bundle.putString(":settings:fragment_args_key", cs)
    intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", bundle)
    }
    this.startActivity(intent)
}