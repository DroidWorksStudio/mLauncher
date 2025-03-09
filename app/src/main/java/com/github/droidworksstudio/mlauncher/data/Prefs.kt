package com.github.droidworksstudio.mlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.github.droidworksstudio.common.showLongToast
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants.Gravity
import com.github.droidworksstudio.mlauncher.helper.getUserHandleFromString
import com.github.droidworksstudio.mlauncher.helper.isSystemInDarkMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val PREFS_FILENAME = "com.github.droidworksstudio.mlauncher"

private const val APP_VERSION = "APP_VERSION"
private const val FIRST_OPEN = "FIRST_OPEN"
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val LOCK_MODE = "LOCK_MODE"
private const val HOME_APPS_NUM = "HOME_APPS_NUM"
private const val HOME_PAGES_NUM = "HOME_PAGES_NUM"
private const val HOME_PAGES_PAGER = "HOME_PAGES_PAGER"
private const val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
private const val AUTO_OPEN_APP = "AUTO_OPEN_APP"
private const val RECENT_APPS_DISPLAYED = "RECENT_APPS_DISPLAYED"
private const val RECENT_COUNTER = "RECENT_COUNTER"
private const val FILTER_STRENGTH = "FILTER_STRENGTH"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val HOME_ALIGNMENT_BOTTOM = "HOME_ALIGNMENT_BOTTOM"
private const val HOME_CLICK_AREA = "HOME_CLICK_AREA"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val CLOCK_ALIGNMENT = "CLOCK_ALIGNMENT"
private const val DATE_ALIGNMENT = "DATE_ALIGNMENT"
private const val ALARM_ALIGNMENT = "ALARM_ALIGNMENT"
private const val DAILY_WORD_ALIGNMENT = "DAILY_WORD_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_BATTERY = "SHOW_BATTERY"
private const val SHOW_BATTERY_ICON = "SHOW_BATTERY_ICON"
private const val SHOW_DATE = "SHOW_DATE"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SETTINGS_LOCKED = "SETTINGS_LOCKED"
private const val HIDE_SEARCH_VIEW = "HIDE_SEARCH_VIEW"
private const val SHOW_CLOCK = "SHOW_CLOCK"
private const val SHOW_CLOCK_FORMAT = "SHOW_CLOCK_FORMAT"
private const val SHOW_ALARM = "SHOW_ALARM"
private const val SHOW_DAILY_WORD = "SHOW_DAILY_WORD"
private const val SHOW_FLOATING = "SHOW_FLOATING"
private const val ICON_PACK = "ICON_PACK"
private const val WORD_LIST = "WORD_LIST"
private const val SEARCH_START = "SEARCH_START"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
private const val LONG_SWIPE_UP_ACTION = "LONG_SWIPE_UP_ACTION"
private const val LONG_SWIPE_DOWN_ACTION = "LONG_SWIPE_DOWN_ACTION"
private const val LONG_SWIPE_RIGHT_ACTION = "LONG_SWIPE_RIGHT_ACTION"
private const val LONG_SWIPE_LEFT_ACTION = "LONG_SWIPE_LEFT_ACTION"
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
private const val CLICK_APP_USAGE_ACTION = "CLICK_APP_USAGE_ACTION"
private const val CLICK_FLOATING_ACTION = "CLICK_FLOATING_ACTION"
private const val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val LOCKED_APPS = "LOCKED_APPS"
private const val SEARCH_ENGINE = "SEARCH_ENGINE"
private const val LAUNCHER_FONT = "LAUNCHER_FONT"
private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"
private const val APP_USAGE_STATS = "APP_USAGE_STATS"
private const val APP_OPACITY = "APP_OPACITY"
private const val APP_LANGUAGE = "APP_LANGUAGE"
private const val APP_THEME = "APP_THEME"
private const val SHORT_SWIPE_UP = "SHORT_SWIPE_UP"
private const val SHORT_SWIPE_DOWN = "SHORT_SWIPE_DOWN"
private const val SHORT_SWIPE_LEFT = "SHORT_SWIPE_LEFT"
private const val SHORT_SWIPE_RIGHT = "SHORT_SWIPE_RIGHT"
private const val LONG_SWIPE_UP = "LONG_SWIPE_UP"
private const val LONG_SWIPE_DOWN = "LONG_SWIPE_DOWN"
private const val LONG_SWIPE_LEFT = "LONG_SWIPE_LEFT"
private const val LONG_SWIPE_RIGHT = "LONG_SWIPE_RIGHT"
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val CLICK_USAGE = "CLICK_USAGE"
private const val CLICK_FLOATING = "CLICK_FLOATING"
private const val CLICK_DATE = "CLICK_DATE"
private const val DOUBLE_TAP = "DOUBLE_TAP"
private const val APP_SIZE_TEXT = "APP_SIZE_TEXT"
private const val DATE_SIZE_TEXT = "DATE_SIZE_TEXT"
private const val CLOCK_SIZE_TEXT = "CLOCK_SIZE_TEXT"
private const val ALARM_SIZE_TEXT = "ALARM_SIZE_TEXT"
private const val DAILY_WORD_SIZE_TEXT = "DAILY_WORD_SIZE_TEXT"
private const val BATTERY_SIZE_TEXT = "BATTERY_SIZE_TEXT"
private const val TEXT_SIZE_SETTINGS = "TEXT_SIZE_SETTINGS"
private const val TEXT_PADDING_SIZE = "TEXT_PADDING_SIZE"

private const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
private const val APP_COLOR = "APP_COLOR"
private const val DATE_COLOR = "DATE_COLOR"
private const val ALARM_CLOCK_COLOR = "ALARM_CLOCK_COLOR"
private const val CLOCK_COLOR = "CLOCK_COLOR"
private const val BATTERY_COLOR = "BATTERY_COLOR"
private const val DAILY_WORD_COLOR = "DAILY_WORD_COLOR"


class Prefs(val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    fun saveToString(): String {
        val all: HashMap<String, Any?> = HashMap(prefs.all)
        return Gson().toJson(all)
    }

    fun loadFromString(json: String) {
        prefs.edit {
            val all: HashMap<String, Any?> =
                Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)
            for ((key, value) in all) {
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Number -> {
                        if (value.toDouble() == value.toInt().toDouble()) {
                            putInt(key, value.toInt())
                        } else {
                            putFloat(key, value.toFloat())
                        }
                    }

                    is MutableSet<*> -> {
                        val list = value.filterIsInstance<String>().toSet()
                        putStringSet(key, list)
                    }

                    else -> {
                        Log.d("backup error", "$value")
                    }
                }
            }
        }
    }

    fun saveToTheme(colorNames: List<String>): String {
        val allPrefs = prefs.all
        val filteredPrefs = mutableMapOf<String, String>()

        for (colorName in colorNames) {
            if (allPrefs.containsKey(colorName)) {
                val colorInt = allPrefs[colorName] as? Int
                if (colorInt != null) {
                    val hexColor = String.format("#%08X", colorInt) // Converts ARGB int to #AARRGGBB
                    filteredPrefs[colorName] = hexColor
                }
            }
        }

        return Gson().toJson(filteredPrefs)
    }

    fun loadFromTheme(json: String) {
        prefs.edit {
            val all: HashMap<String, Any?> =
                Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)

            for ((key, value) in all) {
                try {
                    when (value) {
                        is String -> {
                            if (value.matches(Regex("^#([A-Fa-f0-9]{8})$"))) {
                                // Convert HEX color (#AARRGGBB) to Int safely
                                try {
                                    putInt(key, value.toColorInt())
                                } catch (e: IllegalArgumentException) {
                                    context.showLongToast("Invalid color format for key: $key, value: $value")
                                    Log.e("Theme Import", "Invalid color format for key: $key, value: $value", e)
                                    continue
                                }
                            } else {
                                context.showLongToast("Unsupported value type for key: $key, value: $value")
                                Log.e("Theme Import", "Null value found for key: $key")
                            }
                        }

                        null -> {
                            context.showLongToast("Null value found for key: $key")
                            Log.e("Theme Import", "Null value found for key: $key")
                            continue
                        }

                        else -> {
                            context.showLongToast("Unsupported value type for key: $key, value: $value")
                            Log.e("Theme Import", "Unsupported value type for key: $key, value: $value")
                            continue
                        }
                    }
                } catch (e: Exception) {
                    context.showLongToast("Error processing key: $key, value: $value")
                    Log.e("Theme Import", "Error processing key: $key, value: $value", e)
                }
            }
        }
    }


    var appVersion: Int
        get() = prefs.getInt(APP_VERSION, -1)
        set(value) = prefs.edit { putInt(APP_VERSION, value) }

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_OPEN, value) }

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    var autoOpenApp: Boolean
        get() = prefs.getBoolean(AUTO_OPEN_APP, false)
        set(value) = prefs.edit { putBoolean(AUTO_OPEN_APP, value) }

    var homePager: Boolean
        get() = prefs.getBoolean(HOME_PAGES_PAGER, false)
        set(value) = prefs.edit { putBoolean(HOME_PAGES_PAGER, value) }

    var recentAppsDisplayed: Boolean
        get() = prefs.getBoolean(RECENT_APPS_DISPLAYED, false)
        set(value) = prefs.edit { putBoolean(RECENT_APPS_DISPLAYED, value) }

    var recentCounter: Int
        get() = prefs.getInt(RECENT_COUNTER, 10)
        set(value) = prefs.edit { putInt(RECENT_COUNTER, value) }

    var filterStrength: Int
        get() = prefs.getInt(FILTER_STRENGTH, 25)
        set(value) = prefs.edit { putInt(FILTER_STRENGTH, value) }

    var searchFromStart: Boolean
        get() = prefs.getBoolean(SEARCH_START, false)
        set(value) = prefs.edit { putBoolean(SEARCH_START, value) }

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(AUTO_SHOW_KEYBOARD, value) }

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 4)
        set(value) = prefs.edit { putInt(HOME_APPS_NUM, value) }

    var homePagesNum: Int
        get() = prefs.getInt(HOME_PAGES_NUM, 1)
        set(value) = prefs.edit { putInt(HOME_PAGES_NUM, value) }

    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, getColor(context, getColorInt("bg")))
        set(value) = prefs.edit { putInt(BACKGROUND_COLOR, value) }

    var appColor: Int
        get() = prefs.getInt(APP_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(APP_COLOR, value) }

    var dateColor: Int
        get() = prefs.getInt(DATE_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(DATE_COLOR, value) }

    var clockColor: Int
        get() = prefs.getInt(CLOCK_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(CLOCK_COLOR, value) }

    var batteryColor: Int
        get() = prefs.getInt(BATTERY_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(BATTERY_COLOR, value) }

    var dailyWordColor: Int
        get() = prefs.getInt(DAILY_WORD_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(DAILY_WORD_COLOR, value) }

    var alarmClockColor: Int
        get() = prefs.getInt(ALARM_CLOCK_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(ALARM_CLOCK_COLOR, value) }

    var opacityNum: Int
        get() = prefs.getInt(APP_OPACITY, 255)
        set(value) = prefs.edit { putInt(APP_OPACITY, value) }

    var appUsageStats: Boolean
        get() = prefs.getBoolean(APP_USAGE_STATS, false)
        set(value) = prefs.edit { putBoolean(APP_USAGE_STATS, value) }

    var homeAlignment: Gravity
        get() {
            val string = prefs.getString(
                HOME_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(HOME_ALIGNMENT, value.toString()) }

    var homeAlignmentBottom: Boolean
        get() = prefs.getBoolean(HOME_ALIGNMENT_BOTTOM, false)
        set(value) = prefs.edit { putBoolean(HOME_ALIGNMENT_BOTTOM, value) }

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit { putBoolean(HOME_CLICK_AREA, value) }

    var clockAlignment: Gravity
        get() {
            val string = prefs.getString(
                CLOCK_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(CLOCK_ALIGNMENT, value.toString()) }

    var dateAlignment: Gravity
        get() {
            val string = prefs.getString(
                DATE_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(DATE_ALIGNMENT, value.toString()) }

    var alarmAlignment: Gravity
        get() {
            val string = prefs.getString(
                ALARM_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(ALARM_ALIGNMENT, value.toString()) }

    var dailyWordAlignment: Gravity
        get() {
            val string = prefs.getString(
                DAILY_WORD_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(DAILY_WORD_ALIGNMENT, value.toString()) }

    var drawerAlignment: Gravity
        get() {
            val string = prefs.getString(
                DRAWER_ALIGNMENT,
                Gravity.Right.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit { putString(DRAWER_ALIGNMENT, value.name) }

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit { putBoolean(STATUS_BAR, value) }

    var showDate: Boolean
        get() = prefs.getBoolean(SHOW_DATE, true)
        set(value) = prefs.edit { putBoolean(SHOW_DATE, value) }

    var showClock: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK, true)
        set(value) = prefs.edit { putBoolean(SHOW_CLOCK, value) }

    var showClockFormat: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK_FORMAT, true)
        set(value) = prefs.edit { putBoolean(SHOW_CLOCK_FORMAT, value) }

    var showAlarm: Boolean
        get() = prefs.getBoolean(SHOW_ALARM, false)
        set(value) = prefs.edit { putBoolean(SHOW_ALARM, value) }

    var showDailyWord: Boolean
        get() = prefs.getBoolean(SHOW_DAILY_WORD, false)
        set(value) = prefs.edit { putBoolean(SHOW_DAILY_WORD, value) }

    var showFloating: Boolean
        get() = prefs.getBoolean(SHOW_FLOATING, false)
        set(value) = prefs.edit { putBoolean(SHOW_FLOATING, value) }

    var showBattery: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY, true)
        set(value) = prefs.edit { putBoolean(SHOW_BATTERY, value) }

    var showBatteryIcon: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY_ICON, true)
        set(value) = prefs.edit { putBoolean(SHOW_BATTERY_ICON, value) }

    var iconPack: Constants.IconPacks
        get() {
            return try {
                Constants.IconPacks.valueOf(
                    prefs.getString(
                        ICON_PACK,
                        Constants.IconPacks.Disabled.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.IconPacks.Disabled
            }
        }
        set(value) = prefs.edit { putString(ICON_PACK, value.name) }

    var wordList: String
        get() = prefs.getString(WORD_LIST, "").toString()
        set(value) = prefs.edit { putString(WORD_LIST, value) }

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit { putBoolean(HOME_LOCKED, value) }

    var settingsLocked: Boolean
        get() = prefs.getBoolean(SETTINGS_LOCKED, false)
        set(value) = prefs.edit { putBoolean(SETTINGS_LOCKED, value) }

    var hideSearchView: Boolean
        get() = prefs.getBoolean(HIDE_SEARCH_VIEW, false)
        set(value) = prefs.edit { putBoolean(HIDE_SEARCH_VIEW, value) }

    var shortSwipeUpAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_UP_ACTION,
                        Constants.Action.ShowAppList.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.ShowAppList
            }
        }
        set(value) = prefs.edit { putString(SWIPE_UP_ACTION, value.name) }

    var shortSwipeDownAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_DOWN_ACTION,
                        Constants.Action.ShowNotification.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.ShowNotification
            }
        }
        set(value) = prefs.edit { putString(SWIPE_DOWN_ACTION, value.name) }

    var shortSwipeLeftAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_LEFT_ACTION,
                        Constants.Action.OpenApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenApp
            }
        }
        set(value) = prefs.edit { putString(SWIPE_LEFT_ACTION, value.name) }

    var shortSwipeRightAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_RIGHT_ACTION,
                        Constants.Action.OpenApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenApp
            }
        }
        set(value) = prefs.edit { putString(SWIPE_RIGHT_ACTION, value.name) }

    var longSwipeUpAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        LONG_SWIPE_UP_ACTION,
                        Constants.Action.ShowAppList.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.ShowAppList
            }
        }
        set(value) = prefs.edit { putString(LONG_SWIPE_UP_ACTION, value.name) }

    var longSwipeDownAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        LONG_SWIPE_DOWN_ACTION,
                        Constants.Action.ShowNotification.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.ShowNotification
            }
        }
        set(value) = prefs.edit { putString(LONG_SWIPE_DOWN_ACTION, value.name) }

    var longSwipeLeftAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        LONG_SWIPE_LEFT_ACTION,
                        Constants.Action.LeftPage.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.LeftPage
            }
        }
        set(value) = prefs.edit { putString(LONG_SWIPE_LEFT_ACTION, value.name) }

    var longSwipeRightAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        LONG_SWIPE_RIGHT_ACTION,
                        Constants.Action.RightPage.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.RightPage
            }
        }
        set(value) = prefs.edit { putString(LONG_SWIPE_RIGHT_ACTION, value.name) }

    var clickClockAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        CLICK_CLOCK_ACTION,
                        Constants.Action.OpenApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenApp
            }
        }
        set(value) = prefs.edit { putString(CLICK_CLOCK_ACTION, value.name) }

    var clickAppUsageAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        CLICK_APP_USAGE_ACTION,
                        Constants.Action.ShowDigitalWellbeing.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.ShowDigitalWellbeing
            }
        }
        set(value) = prefs.edit { putString(CLICK_APP_USAGE_ACTION, value.name) }

    var clickFloatingAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        CLICK_FLOATING_ACTION,
                        Constants.Action.Disabled.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.Disabled
            }
        }
        set(value) = prefs.edit { putString(CLICK_FLOATING_ACTION, value.name) }

    var clickDateAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        CLICK_DATE_ACTION,
                        Constants.Action.OpenApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenApp
            }
        }
        set(value) = prefs.edit { putString(CLICK_DATE_ACTION, value.name) }

    var doubleTapAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        DOUBLE_TAP_ACTION,
                        Constants.Action.RestartApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.RestartApp
            }
        }
        set(value) = prefs.edit { putString(DOUBLE_TAP_ACTION, value.name) }

    var appTheme: Constants.Theme
        get() {
            return try {
                Constants.Theme.valueOf(
                    prefs.getString(APP_THEME, Constants.Theme.System.name).toString()
                )
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) = prefs.edit { putString(APP_THEME, value.name) }

    var appLanguage: Constants.Language
        get() {
            return try {
                Constants.Language.valueOf(
                    prefs.getString(
                        APP_LANGUAGE,
                        Constants.Language.System.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Language.System
            }
        }
        set(value) = prefs.edit { putString(APP_LANGUAGE, value.name) }

    var searchEngines: Constants.SearchEngines
        get() {
            return try {
                Constants.SearchEngines.valueOf(
                    prefs.getString(
                        SEARCH_ENGINE,
                        Constants.SearchEngines.Google.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.SearchEngines.Google
            }
        }
        set(value) = prefs.edit { putString(SEARCH_ENGINE, value.name) }

    var fontFamily: Constants.FontFamily
        get() {
            return try {
                Constants.FontFamily.valueOf(
                    prefs.getString(
                        LAUNCHER_FONT,
                        Constants.FontFamily.System.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.FontFamily.System
            }
        }
        set(value) = prefs.edit { putString(LAUNCHER_FONT, value.name) }

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value) }

    var lockedApps: MutableSet<String>
        get() = prefs.getStringSet(LOCKED_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(LOCKED_APPS, value) }

    /**
     * By the number in home app list, get the list item.
     * TODO why not just save it as a list?
     */
    fun getHomeAppModel(i: Int): AppListItem {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appListItem: AppListItem) {
        storeApp("$i", appListItem)
    }

    fun setHomeAppName(i: Int, name: String) {
        val nameId = "${APP_NAME}_$i"
        prefs.edit { putString(nameId, name) }
    }

    var appShortSwipeUp: AppListItem
        get() = loadApp(SHORT_SWIPE_UP)
        set(appModel) = storeApp(SHORT_SWIPE_UP, appModel)
    var appShortSwipeDown: AppListItem
        get() = loadApp(SHORT_SWIPE_DOWN)
        set(appModel) = storeApp(SHORT_SWIPE_DOWN, appModel)
    var appShortSwipeLeft: AppListItem
        get() = loadApp(SHORT_SWIPE_LEFT)
        set(appModel) = storeApp(SHORT_SWIPE_LEFT, appModel)
    var appShortSwipeRight: AppListItem
        get() = loadApp(SHORT_SWIPE_RIGHT)
        set(appModel) = storeApp(SHORT_SWIPE_RIGHT, appModel)

    var appLongSwipeUp: AppListItem
        get() = loadApp(LONG_SWIPE_UP)
        set(appModel) = storeApp(LONG_SWIPE_UP, appModel)
    var appLongSwipeDown: AppListItem
        get() = loadApp(LONG_SWIPE_DOWN)
        set(appModel) = storeApp(LONG_SWIPE_DOWN, appModel)
    var appLongSwipeLeft: AppListItem
        get() = loadApp(LONG_SWIPE_LEFT)
        set(appModel) = storeApp(LONG_SWIPE_LEFT, appModel)
    var appLongSwipeRight: AppListItem
        get() = loadApp(LONG_SWIPE_RIGHT)
        set(appModel) = storeApp(LONG_SWIPE_RIGHT, appModel)

    var appClickClock: AppListItem
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickUsage: AppListItem
        get() = loadApp(CLICK_USAGE)
        set(appModel) = storeApp(CLICK_USAGE, appModel)

    var appFloating: AppListItem
        get() = loadApp(CLICK_FLOATING)
        set(appModel) = storeApp(CLICK_FLOATING, appModel)

    var appClickDate: AppListItem
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)

    var appDoubleTap: AppListItem
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)

    /**
     *  Restore an `AppListItem` from preferences.
     *
     *  We store not only application name, but everything needed to start the item.
     *  Because thus we save time to query the system about it?
     *
     *  TODO store with protobuf instead of serializing manually.
     */
    private fun loadApp(id: String): AppListItem {
        val appName = prefs.getString("${APP_NAME}_$id", "").toString()
        val appPackage = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val appAlias = prefs.getString("${APP_ALIAS}_$id", "").toString()
        val appActivityName = prefs.getString("${APP_ACTIVITY}_$id", "").toString()

        val userHandleString = try {
            prefs.getString("${APP_USER}_$id", "").toString()
        } catch (_: Exception) {
            ""
        }
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)

        return AppListItem(
            activityLabel = appName,
            activityPackage = appPackage,
            customLabel = appAlias,
            activityClass = appActivityName,
            user = userHandle,
        )
    }

    private fun storeApp(id: String, app: AppListItem) {
        prefs.edit {

            putString("${APP_NAME}_$id", app.label)
            putString("${APP_PACKAGE}_$id", app.activityPackage)
            putString("${APP_ACTIVITY}_$id", app.activityClass)
            putString("${APP_ALIAS}_$id", app.customLabel)
            putString("${APP_USER}_$id", app.user.toString())
        }
    }

    var appSize: Int
        get() {
            return try {
                prefs.getInt(APP_SIZE_TEXT, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit { putInt(APP_SIZE_TEXT, value) }

    var dateSize: Int
        get() {
            return try {
                prefs.getInt(DATE_SIZE_TEXT, 22)
            } catch (_: Exception) {
                22
            }
        }
        set(value) = prefs.edit { putInt(DATE_SIZE_TEXT, value) }

    var clockSize: Int
        get() {
            return try {
                prefs.getInt(CLOCK_SIZE_TEXT, 42)
            } catch (_: Exception) {
                42
            }
        }
        set(value) = prefs.edit { putInt(CLOCK_SIZE_TEXT, value) }

    var alarmSize: Int
        get() {
            return try {
                prefs.getInt(ALARM_SIZE_TEXT, 20)
            } catch (_: Exception) {
                20
            }
        }
        set(value) = prefs.edit { putInt(ALARM_SIZE_TEXT, value) }

    var dailyWordSize: Int
        get() {
            return try {
                prefs.getInt(DAILY_WORD_SIZE_TEXT, 20)
            } catch (_: Exception) {
                20
            }
        }
        set(value) = prefs.edit { putInt(DAILY_WORD_SIZE_TEXT, value) }


    var batterySize: Int
        get() {
            return try {
                prefs.getInt(BATTERY_SIZE_TEXT, 14)
            } catch (_: Exception) {
                14
            }
        }
        set(value) = prefs.edit { putInt(BATTERY_SIZE_TEXT, value) }

    var settingsSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_SETTINGS, 12)
            } catch (_: Exception) {
                12
            }
        }
        set(value) = prefs.edit { putInt(TEXT_SIZE_SETTINGS, value) }

    var textPaddingSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_PADDING_SIZE, 10)
            } catch (_: Exception) {
                10
            }
        }
        set(value) = prefs.edit { putInt(TEXT_PADDING_SIZE, value) }

    private fun getColorInt(type: String): Int {
        when (appTheme) {
            Constants.Theme.System -> {
                return if (isSystemInDarkMode(context)) {
                    if (type == "bg") R.color.black
                    else R.color.white
                } else {
                    if (type == "bg") R.color.white
                    else R.color.black
                }
            }

            Constants.Theme.Dark -> {
                return if (type == "bg") R.color.black
                else R.color.white
            }

            Constants.Theme.Light -> {
                return if (type == "bg") R.color.white
                else R.color.black
            }
        }
    }

    // return app label
    fun getAppName(location: Int): String {
        return getHomeAppModel(location).activityLabel
    }

    fun getAppAlias(appName: String): String {
        return prefs.getString(appName, "").toString()
    }

    fun setAppAlias(appPackage: String, appAlias: String) {
        prefs.edit { putString(appPackage, appAlias) }
    }

    fun remove(prefName: String) {
        prefs.edit { remove(prefName) }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}
