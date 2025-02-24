package com.github.droidworksstudio.mlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat.getColor
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
        val editor = prefs.edit()
        val all: HashMap<String, Any?> =
            Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)
        for ((key, value) in all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Number -> {
                    if (value.toDouble() == value.toInt().toDouble()) {
                        editor.putInt(key, value.toInt())
                    } else {
                        editor.putFloat(key, value.toFloat())
                    }
                }

                is MutableSet<*> -> {
                    val list = value.filterIsInstance<String>().toSet()
                    editor.putStringSet(key, list)
                }

                else -> {
                    Log.d("backup error", "$value")
                }
            }
        }
        editor.apply()
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
        val editor = prefs.edit()
        val all: HashMap<String, Any?> =
            Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)

        for ((key, value) in all) {
            try {
                when (value) {
                    is String -> {
                        if (value.matches(Regex("^#([A-Fa-f0-9]{8})$"))) {
                            // Convert HEX color (#AARRGGBB) to Int safely
                            try {
                                editor.putInt(key, Color.parseColor(value))
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
        editor.apply()
    }


    var appVersion: Int
        get() = prefs.getInt(APP_VERSION, -1)
        set(value) = prefs.edit().putInt(APP_VERSION, value).apply()

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_OPEN, value).apply()

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_SETTINGS_OPEN, value).apply()

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit().putBoolean(LOCK_MODE, value).apply()

    var autoOpenApp: Boolean
        get() = prefs.getBoolean(AUTO_OPEN_APP, false)
        set(value) = prefs.edit().putBoolean(AUTO_OPEN_APP, value).apply()

    var homePager: Boolean
        get() = prefs.getBoolean(HOME_PAGES_PAGER, false)
        set(value) = prefs.edit().putBoolean(HOME_PAGES_PAGER, value).apply()

    var recentAppsDisplayed: Boolean
        get() = prefs.getBoolean(RECENT_APPS_DISPLAYED, false)
        set(value) = prefs.edit().putBoolean(RECENT_APPS_DISPLAYED, value).apply()

    var recentCounter: Int
        get() = prefs.getInt(RECENT_COUNTER, 10)
        set(value) = prefs.edit().putInt(RECENT_COUNTER, value).apply()

    var filterStrength: Int
        get() = prefs.getInt(FILTER_STRENGTH, 25)
        set(value) = prefs.edit().putInt(FILTER_STRENGTH, value).apply()

    var searchFromStart: Boolean
        get() = prefs.getBoolean(SEARCH_START, false)
        set(value) = prefs.edit().putBoolean(SEARCH_START, value).apply()

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit().putBoolean(AUTO_SHOW_KEYBOARD, value).apply()

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 4)
        set(value) = prefs.edit().putInt(HOME_APPS_NUM, value).apply()

    var homePagesNum: Int
        get() = prefs.getInt(HOME_PAGES_NUM, 1)
        set(value) = prefs.edit().putInt(HOME_PAGES_NUM, value).apply()

    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, getColor(context, getColorInt("bg")))
        set(value) = prefs.edit().putInt(BACKGROUND_COLOR, value).apply()

    var appColor: Int
        get() = prefs.getInt(APP_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(APP_COLOR, value).apply()

    var dateColor: Int
        get() = prefs.getInt(DATE_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(DATE_COLOR, value).apply()

    var clockColor: Int
        get() = prefs.getInt(CLOCK_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(CLOCK_COLOR, value).apply()

    var batteryColor: Int
        get() = prefs.getInt(BATTERY_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(BATTERY_COLOR, value).apply()

    var dailyWordColor: Int
        get() = prefs.getInt(DAILY_WORD_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(DAILY_WORD_COLOR, value).apply()

    var alarmClockColor: Int
        get() = prefs.getInt(ALARM_CLOCK_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit().putInt(ALARM_CLOCK_COLOR, value).apply()

    var opacityNum: Int
        get() = prefs.getInt(APP_OPACITY, 255)
        set(value) = prefs.edit().putInt(APP_OPACITY, value).apply()

    var appUsageStats: Boolean
        get() = prefs.getBoolean(APP_USAGE_STATS, false)
        set(value) = prefs.edit().putBoolean(APP_USAGE_STATS, value).apply()

    var homeAlignment: Gravity
        get() {
            val string = prefs.getString(
                HOME_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(HOME_ALIGNMENT, value.toString()).apply()

    var homeAlignmentBottom: Boolean
        get() = prefs.getBoolean(HOME_ALIGNMENT_BOTTOM, false)
        set(value) = prefs.edit().putBoolean(HOME_ALIGNMENT_BOTTOM, value).apply()

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit().putBoolean(HOME_CLICK_AREA, value).apply()

    var clockAlignment: Gravity
        get() {
            val string = prefs.getString(
                CLOCK_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(CLOCK_ALIGNMENT, value.toString()).apply()

    var dateAlignment: Gravity
        get() {
            val string = prefs.getString(
                DATE_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(DATE_ALIGNMENT, value.toString()).apply()

    var alarmAlignment: Gravity
        get() {
            val string = prefs.getString(
                ALARM_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(ALARM_ALIGNMENT, value.toString()).apply()

    var dailyWordAlignment: Gravity
        get() {
            val string = prefs.getString(
                DAILY_WORD_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(DAILY_WORD_ALIGNMENT, value.toString()).apply()

    var drawerAlignment: Gravity
        get() {
            val string = prefs.getString(
                DRAWER_ALIGNMENT,
                Gravity.Right.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(DRAWER_ALIGNMENT, value.name).apply()

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit().putBoolean(STATUS_BAR, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(SHOW_DATE, value).apply()

    var showClock: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK, true)
        set(value) = prefs.edit().putBoolean(SHOW_CLOCK, value).apply()

    var showClockFormat: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK_FORMAT, true)
        set(value) = prefs.edit().putBoolean(SHOW_CLOCK_FORMAT, value).apply()

    var showAlarm: Boolean
        get() = prefs.getBoolean(SHOW_ALARM, false)
        set(value) = prefs.edit().putBoolean(SHOW_ALARM, value).apply()

    var showDailyWord: Boolean
        get() = prefs.getBoolean(SHOW_DAILY_WORD, false)
        set(value) = prefs.edit().putBoolean(SHOW_DAILY_WORD, value).apply()

    var showFloating: Boolean
        get() = prefs.getBoolean(SHOW_FLOATING, false)
        set(value) = prefs.edit().putBoolean(SHOW_FLOATING, value).apply()

    var showBattery: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(SHOW_BATTERY, value).apply()

    var showBatteryIcon: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY_ICON, true)
        set(value) = prefs.edit().putBoolean(SHOW_BATTERY_ICON, value).apply()

    var iconPack: Constants.IconPacks
        get() = loadIconPacks()
        set(value) = storeIconPacks(value)

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit().putBoolean(HOME_LOCKED, value).apply()

    var settingsLocked: Boolean
        get() = prefs.getBoolean(SETTINGS_LOCKED, false)
        set(value) = prefs.edit().putBoolean(SETTINGS_LOCKED, value).apply()

    var hideSearchView: Boolean
        get() = prefs.getBoolean(HIDE_SEARCH_VIEW, false)
        set(value) = prefs.edit().putBoolean(HIDE_SEARCH_VIEW, value).apply()

    var shortSwipeUpAction: Constants.Action
        get() = loadAction(SWIPE_UP_ACTION, Constants.Action.ShowAppList)
        set(value) = storeAction(SWIPE_UP_ACTION, value)

    var shortSwipeDownAction: Constants.Action
        get() = loadAction(SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
        set(value) = storeAction(SWIPE_DOWN_ACTION, value)

    var shortSwipeLeftAction: Constants.Action
        get() = loadAction(SWIPE_LEFT_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(SWIPE_LEFT_ACTION, value)

    var shortSwipeRightAction: Constants.Action
        get() = loadAction(SWIPE_RIGHT_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(SWIPE_RIGHT_ACTION, value)

    var longSwipeUpAction: Constants.Action
        get() = loadAction(LONG_SWIPE_UP_ACTION, Constants.Action.ShowAppList)
        set(value) = storeAction(LONG_SWIPE_UP_ACTION, value)

    var longSwipeDownAction: Constants.Action
        get() = loadAction(LONG_SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
        set(value) = storeAction(LONG_SWIPE_DOWN_ACTION, value)

    var longSwipeLeftAction: Constants.Action
        get() = loadAction(LONG_SWIPE_LEFT_ACTION, Constants.Action.LeftPage)
        set(value) = storeAction(LONG_SWIPE_LEFT_ACTION, value)

    var longSwipeRightAction: Constants.Action
        get() = loadAction(LONG_SWIPE_RIGHT_ACTION, Constants.Action.RightPage)
        set(value) = storeAction(LONG_SWIPE_RIGHT_ACTION, value)

    var clickClockAction: Constants.Action
        get() = loadAction(CLICK_CLOCK_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_CLOCK_ACTION, value)

    var clickAppUsageAction: Constants.Action
        get() = loadAction(CLICK_APP_USAGE_ACTION, Constants.Action.ShowDigitalWellbeing)
        set(value) = storeAction(CLICK_APP_USAGE_ACTION, value)

    var clickFloatingAction: Constants.Action
        get() = loadAction(CLICK_FLOATING_ACTION, Constants.Action.Disabled)
        set(value) = storeAction(CLICK_FLOATING_ACTION, value)

    var clickDateAction: Constants.Action
        get() = loadAction(CLICK_DATE_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_DATE_ACTION, value)

    var doubleTapAction: Constants.Action
        get() = loadAction(DOUBLE_TAP_ACTION, Constants.Action.RestartApp)
        set(value) = storeAction(DOUBLE_TAP_ACTION, value)

    private fun loadAction(prefString: String, default: Constants.Action): Constants.Action {
        val string = prefs.getString(
            prefString,
            default.toString()
        ).toString()
        return Constants.Action.valueOf(string)
    }

    private fun storeAction(prefString: String, value: Constants.Action) {
        prefs.edit().putString(prefString, value.name).apply()
    }

    private fun storeIconPacks(value: Constants.IconPacks) {
        prefs.edit().putString(ICON_PACK, value.name).apply()
    }

    private fun loadIconPacks(): Constants.IconPacks {
        val string = prefs.getString(
            ICON_PACK,
            Constants.IconPacks.Disabled.toString()
        ).toString()
        return Constants.IconPacks.valueOf(string)
    }

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
        set(value) = prefs.edit().putString(APP_THEME, value.name).apply()

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
        set(value) = prefs.edit().putString(APP_LANGUAGE, value.name).apply()

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
        set(value) = prefs.edit().putString(SEARCH_ENGINE, value.name).apply()

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
        set(value) = prefs.edit().putString(LAUNCHER_FONT, value.name).apply()

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

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
        prefs.edit().putString(nameId, name).apply()
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
        val edit = prefs.edit()

        edit.putString("${APP_NAME}_$id", app.label)
        edit.putString("${APP_PACKAGE}_$id", app.activityPackage)
        edit.putString("${APP_ACTIVITY}_$id", app.activityClass)
        edit.putString("${APP_ALIAS}_$id", app.customLabel) // TODO can be empty. so what?
        edit.putString("${APP_USER}_$id", app.user.toString())
        edit.apply()
    }

    var appSize: Int
        get() {
            return try {
                prefs.getInt(APP_SIZE_TEXT, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(APP_SIZE_TEXT, value).apply()

    var dateSize: Int
        get() {
            return try {
                prefs.getInt(DATE_SIZE_TEXT, 22)
            } catch (_: Exception) {
                22
            }
        }
        set(value) = prefs.edit().putInt(DATE_SIZE_TEXT, value).apply()

    var clockSize: Int
        get() {
            return try {
                prefs.getInt(CLOCK_SIZE_TEXT, 42)
            } catch (_: Exception) {
                42
            }
        }
        set(value) = prefs.edit().putInt(CLOCK_SIZE_TEXT, value).apply()

    var alarmSize: Int
        get() {
            return try {
                prefs.getInt(ALARM_SIZE_TEXT, 20)
            } catch (_: Exception) {
                20
            }
        }
        set(value) = prefs.edit().putInt(ALARM_SIZE_TEXT, value).apply()

    var dailyWordSize: Int
        get() {
            return try {
                prefs.getInt(DAILY_WORD_SIZE_TEXT, 20)
            } catch (_: Exception) {
                20
            }
        }
        set(value) = prefs.edit().putInt(DAILY_WORD_SIZE_TEXT, value).apply()


    var batterySize: Int
        get() {
            return try {
                prefs.getInt(BATTERY_SIZE_TEXT, 14)
            } catch (_: Exception) {
                14
            }
        }
        set(value) = prefs.edit().putInt(BATTERY_SIZE_TEXT, value).apply()

    var settingsSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_SETTINGS, 12)
            } catch (_: Exception) {
                12
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE_SETTINGS, value).apply()

    var textPaddingSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_PADDING_SIZE, 10)
            } catch (_: Exception) {
                10
            }
        }
        set(value) = prefs.edit().putInt(TEXT_PADDING_SIZE, value).apply()

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
        prefs.edit().putString(appPackage, appAlias).apply()
    }

    fun remove(prefName: String) {
        prefs.edit().remove(prefName).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
