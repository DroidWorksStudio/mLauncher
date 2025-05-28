package com.github.droidworksstudio.launcher.settings

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.widget.TextView
import androidx.preference.PreferenceManager

class SharedPreferenceManager(private val context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    // General UI
    fun getBgColor(): Int {
        val bgColor = preferences.getString("bgColor", "#00000000")
        if (bgColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        }
        return Color.parseColor(bgColor)
    }

    fun getTextColor(): Int {
        val textColor = preferences.getString("textColor", "#FFF3F3F3")
        if (textColor == "material") {
            return getThemeColor(com.google.android.material.R.attr.colorPrimary)
        }
        return Color.parseColor(textColor)
    }


    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
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

    // Home Screen
    fun getClockAlignment(): String? {
        return preferences.getString("clockAlignment", "left")
    }

    fun getClockSize(): String? {
        return preferences.getString("clockSize", "medium")
    }

    fun getDateSize(): String? {
        return preferences.getString("dateSize", "medium")
    }

    fun setShortcut(textView: TextView, packageName: String, profile: Int) {
        val editor = preferences.edit()
        editor.putString("shortcut${textView.id}", "$packageName§splitter§$profile§splitter§${textView.text}")
        editor.apply()
    }

    fun getShortcut(textView: TextView): List<String>? {
        val value = preferences.getString("shortcut${textView.id}", "e§splitter§e")
        return value?.split("§splitter§")
    }

    fun getShortcutNumber(): Int? {
        return preferences.getString("shortcutNo", "4")?.toInt()
    }

    fun getShortcutAlignment(): String? {
        return preferences.getString("shortcutAlignment", "left")
    }

    fun getShortcutSize(): String? {
        return preferences.getString("shortcutSize", "medium")
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

    fun setWeatherLocation(location: String, region: String?) {
        val editor = preferences.edit()
        editor.putString("location", location)
        editor.putString("locationRegion", region)
        editor.apply()
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

    // Gestures
    fun setGestures(direction: String, appName: String?) {
        val editor = preferences.edit()
        editor.putString("${direction}SwipeApp", appName)
        editor.apply()
    }

    fun getGestureName(direction: String): String? {
        val name = preferences.getString("${direction}SwipeApp", "")?.split("§splitter§")
        return name?.get(0)
    }

    fun getGestureInfo(direction: String): List<String>? {
        return preferences.getString("${direction}SwipeApp", "")?.split("§splitter§")
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

    fun getSearchAlignment(): String? {
        return preferences.getString("searchAlignment", "left")
    }

    fun getSearchSize(): String? {
        return preferences.getString("searchSize", "medium")
    }

    fun isAutoKeyboardEnabled(): Boolean {
        return preferences.getBoolean("autoKeyboard", false)
    }

    // Hidden Apps
    fun setAppHidden(packageName: String, profile: Int, hidden: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean("hidden$packageName-$profile", hidden)
        editor.apply()
    }

    fun isAppHidden(packageName: String, profile: Int): Boolean {
        return preferences.getBoolean("hidden$packageName-$profile", false) // Default to false (visible)
    }

    fun setAppVisible(packageName: String, profile: Int) {
        val editor = preferences.edit()
        editor.remove("hidden$packageName-$profile")
        editor.apply()
    }

    //Renaming apps
    fun setAppName(packageName: String, profile: Int, newName: String) {
        val editor = preferences.edit()
        editor.putString("name$packageName-$profile", newName)
        editor.apply()
    }

    fun getAppName(packageName: String, profile: Int, appName: CharSequence): CharSequence? {
        return preferences.getString("name$packageName-$profile", appName.toString())
    }

    fun resetAppName(packageName: String, profile: Int) {
        val editor = preferences.edit()
        editor.remove("name$packageName-$profile")
        editor.apply()
    }
}