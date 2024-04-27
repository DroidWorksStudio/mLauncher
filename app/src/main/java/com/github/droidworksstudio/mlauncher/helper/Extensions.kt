package com.github.droidworksstudio.mlauncher.helper

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.UserHandle
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import com.github.droidworksstudio.mlauncher.data.Constants

fun View.hideKeyboard() {
    this.clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(show: Boolean = true) {
    if (show.not()) return
    if (this.requestFocus())
        postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }, 100)
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


fun Context.isPackageInstalled(packageName: String, userHandle: UserHandle = android.os.Process.myUserHandle()): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.size > 0
}

fun Context.isFeaturePhone(): Boolean {
    val viewConfig = ViewConfiguration.get(this)

    // Check for physical navigation buttons
    val hasPhysicalNavButtons = viewConfig.hasPermanentMenuKey()

    // Check for physical keyboard
    val configuration = this.resources.configuration
    val hasPhysicalKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
            configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    // Check for gyroscope
    val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val hasGyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

    // Return true if all three features are present
    return hasPhysicalNavButtons && hasPhysicalKeyboard && hasGyroscope
}
