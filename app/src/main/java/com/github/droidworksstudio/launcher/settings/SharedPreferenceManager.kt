package com.github.droidworksstudio.launcher.settings

import android.app.AlertDialog
import android.content.Context
import android.util.TypedValue
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import com.github.droidworksstudio.launcher.R

class SharedPreferenceManager(private val context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    // General UI
    fun getBgColor(): Int {
        val bgColor = preferences.getString("bgColor", "#00000000")
        if (bgColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        }
        return bgColor!!.toColorInt()
    }

    fun getTextColor(): Int {
        val textColor = getTextString()
        if (textColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorPrimary)
        }
        return textColor!!.toColorInt()
    }

    fun getTextString(): String? {
        return preferences.getString("textColor", "#FFF3F3F3")
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun getTextFont(): String? {
        return preferences.getString("textFont", "system")
    }

    fun getTextStyle(): String? {
        return preferences.getString("textStyle", "normal")
    }

    fun isBarVisible(): Boolean {
        return preferences.getBoolean("barVisibility", false)
    }

    fun getAnimationSpeed(): Long {
        val animSpeed = preferences.getString("animationSpeed", "200")?.toLong()
        if (animSpeed != null) {
            return animSpeed
        }
        return 200
    }

    fun getSwipeThreshold(): Int {
        return preferences.getString("swipeThreshold", "100")?.toInt() ?: 100
    }

    fun getSwipeVelocity(): Int {
        return preferences.getString("swipeVelocity", "100")?.toInt() ?: 100
    }

    fun isConfirmationEnabled(): Boolean {
        return preferences.getBoolean("enableConfirmation", false)
    }

    // Home Screen
    fun isClockEnabled(): Boolean {
        return preferences.getBoolean("clockEnabled", true)
    }

    fun getClockAlignment(): String? {
        return preferences.getString("clockAlignment", "left")
    }

    fun getClockSize(): String? {
        return preferences.getString("clockSize", "medium")
    }

    fun isDateEnabled(): Boolean {
        return preferences.getBoolean("dateEnabled", true)
    }

    fun getDateSize(): String? {
        return preferences.getString("dateSize", "medium")
    }

    fun setShortcut(index: Int, text: CharSequence, componentName: String, profile: Int, isContact: Boolean = false) {
        preferences.edit {
            putString("shortcut${index}", "$componentName:::split:::$profile:::split:::${text}:::split:::${isContact}")
        }
    }

    fun getShortcut(index: Int): List<String>? {
        val value = preferences.getString("shortcut${index}", "e:::split:::e:::split:::e:::split:::e")
        return value?.split(":::split:::")
    }

    fun getShortcutNumber(): Int? {
        return preferences.getString("shortcutNo", "4")?.toInt()
    }

    fun getShortcutAlignment(): String? {
        return preferences.getString("shortcutAlignment", "left")
    }

    fun getShortcutVAlignment(): String? {
        return preferences.getString("shortcutVAlignment", "center")
    }

    fun getShortcutSize(): String? {
        return preferences.getString("shortcutSize", "medium")
    }

    fun getShortcutWeight(): Float? {
        return preferences.getString("shortcutWeight", "0.09")?.toFloat()
    }

    fun areShortcutsLocked(): Boolean {
        return preferences.getBoolean("lockShortcuts", false)
    }

    // Show hidden apps in shortcut selection
    fun showHiddenShortcuts(): Boolean {
        return preferences.getBoolean("showHiddenShortcuts", true)
    }

    fun setPinnedApp(componentName: String, profile: Int) {
        preferences.edit {

            val pinnedAppString = when (isAppPinned(componentName, profile)) {
                true -> {
                    getPinnedAppString()?.replace(":::section:::$componentName:::split:::$profile", "")
                }

                false -> {
                    "${getPinnedAppString()}:::section:::$componentName:::split:::$profile"
                }
            }

            putString(
                "pinnedApps",
                pinnedAppString
            )
        }
    }

    private fun getPinnedAppString(): String? {
        return preferences.getString("pinnedApps", "")
    }

    private fun getPinnedApps(): List<Pair<String, Int?>> {
        val pinnedApps = mutableListOf<Pair<String, Int?>>()
        val pinnedAppList = getPinnedAppString()?.split(":::section:::")

        pinnedAppList?.forEach {
            val app = it.split(":::split:::")
            if (app.size > 1) {
                pinnedApps.add(Pair(app[0], app[1].toIntOrNull()))
            }
        }

        return pinnedApps
    }

    fun isAppPinned(componentName: String, profile: Int): Boolean {
        return getPinnedApps().contains(Pair(componentName, profile))
    }

    fun isBatteryEnabled(): Boolean {
        return preferences.getBoolean("batteryEnabled", false)
    }

    // Weather
    fun isWeatherEnabled(): Boolean {
        return preferences.getBoolean("weatherEnabled", false)
    }

    fun isWeatherGPS(): Boolean {
        return preferences.getBoolean("gpsLocation", false)
    }

    fun setWeatherGPS(isEnabled: Boolean) {
        preferences.edit {
            putBoolean("gpsLocation", isEnabled)
        }
    }

    fun setWeatherLocation(location: String, region: String?) {
        preferences.edit {
            putString("location", location)
            putString("locationRegion", region)
        }
    }

    fun getWeatherLocation(): String? {
        return preferences.getString("location", "")
    }

    fun getWeatherRegion(): String? {
        return preferences.getString("locationRegion", "")
    }

    fun getTempUnits(): String? {
        return preferences.getString("tempUnits", "celsius")
    }

    fun isClockGestureEnabled(): Boolean {
        return preferences.getBoolean("clockClick", true)
    }

    fun isDateGestureEnabled(): Boolean {
        return preferences.getBoolean("dateClick", true)
    }

    // Gestures
    fun setGestures(direction: String, appInfo: String?) {
        preferences.edit {
            putString("${direction}SwipeApp", appInfo)
        }
    }

    fun getGestureName(direction: String): String? {
        val name = preferences.getString("${direction}SwipeApp", "")?.split(":::split:::")
        return name?.get(0)
    }

    fun getGestureInfo(direction: String): List<String>? {
        return preferences.getString("${direction}SwipeApp", "")?.split(":::split:::")
    }

    fun isGestureEnabled(direction: String): Boolean {
        return preferences.getBoolean("${direction}Swipe", false)
    }

    fun isDoubleTapEnabled(): Boolean {
        return preferences.getBoolean("doubleTap", false)
    }

    // Application Menu
    fun getAppAlignment(): String? {
        return preferences.getString("appMenuAlignment", "left")
    }

    fun getAppSize(): String? {
        return preferences.getString("appMenuSize", "medium")
    }

    fun isPinEnabled(): Boolean {
        return preferences.getBoolean("pinEnabled", true)
    }

    fun isInfoEnabled(): Boolean {
        return preferences.getBoolean("infoEnabled", false)
    }

    fun isUninstallEnabled(): Boolean {
        return preferences.getBoolean("uninstallEnabled", true)
    }

    fun isRenameEnabled(): Boolean {
        return preferences.getBoolean("renameEnabled", true)
    }

    fun isHideEnabled(): Boolean {
        return preferences.getBoolean("hideEnabled", true)
    }

    fun isCloseEnabled(): Boolean {
        return preferences.getBoolean("closeEnabled", true)
    }

    fun isSearchEnabled(): Boolean {
        return preferences.getBoolean("searchEnabled", true)
    }

    fun getSearchAlignment(): String? {
        return preferences.getString("searchAlignment", "left")
    }

    fun getSearchSize(): String? {
        return preferences.getString("searchSize", "medium")
    }

    fun isFuzzySearchEnabled(): Boolean {
        return preferences.getBoolean("fuzzySearchEnabled", false)
    }

    fun getAppSpacing(): Int? {
        return preferences.getString("appSpacing", "20")?.toInt()
    }

    fun isAutoKeyboardEnabled(): Boolean {
        return preferences.getBoolean("autoKeyboard", false)
    }

    fun isAutoLaunchEnabled(): Boolean {
        return preferences.getBoolean("autoLaunch", false)
    }

    fun areContactsEnabled(): Boolean {
        return preferences.getBoolean("contactsEnabled", false)
    }

    fun setContactsEnabled(isEnabled: Boolean) {
        preferences.edit {
            putBoolean("contactsEnabled", isEnabled)
        }
    }

    fun isWebSearchEnabled(): Boolean {
        return preferences.getBoolean("webSearchEnabled", false) && isSearchEnabled() && !isAutoLaunchEnabled()
    }

    fun isGooglePlaySearchEnabled(): Boolean {
        return preferences.getBoolean("googlePlaySearchEnabled", false) && isSearchEnabled() && !isAutoLaunchEnabled()
    }

    // Hidden Apps
    fun setAppHidden(componentName: String, profile: Int, hidden: Boolean) {
        preferences.edit {
            putBoolean("hidden$componentName-$profile", hidden)
        }
    }

    fun isAppHidden(componentName: String, profile: Int): Boolean {
        return preferences.getBoolean("hidden$componentName-$profile", false) // Default to false (visible)
    }

    fun setAppVisible(componentName: String, profile: Int) {
        preferences.edit {
            remove("hidden$componentName-$profile")
        }
    }

    //Renaming apps
    fun setAppName(componentName: String, profile: Int, newName: String) {
        preferences.edit {
            putString("name$componentName-$profile", newName)
        }
    }

    fun getAppName(componentName: String, profile: Int, appName: CharSequence): CharSequence? {
        return preferences.getString("name$componentName-$profile", appName.toString())
    }

    fun resetAppName(componentName: String, profile: Int) {
        preferences.edit {
            remove("name$componentName-$profile")
        }
    }

    fun resetAllPreferences() {
        AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.confirm_title))
            setMessage(context.getString(R.string.reset_setting_confirm))
            setPositiveButton(context.getString(R.string.confirm_yes)) { _, _ ->
                performReset()
            }

            setNegativeButton(context.getString(R.string.confirm_no)) { _, _ ->
            }
        }.create().show()
    }

    private fun performReset() {
        preferences.edit {
            clear()
            putBoolean("isRestored", true)
        }
    }
}