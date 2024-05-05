package com.github.droidworksstudio.mlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.util.Log
import com.github.droidworksstudio.mlauncher.data.Constants.Gravity
import com.github.droidworksstudio.mlauncher.helper.getUserHandleFromString
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val APP_LANGUAGE = "app_language"
private const val PREFS_FILENAME = "com.github.droidworksstudio.mlauncher"

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
private const val HOME_FOLLOW_ACCENT = "HOME_FOLLOW_ACCENT"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val TIME_ALIGNMENT = "TIME_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_BATTERY = "SHOW_BATTERY"
private const val SHOW_BATTERY_ICON = "SHOW_BATTERY_ICON"
private const val SHOW_DATE = "SHOW_DATE"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SETTINGS_LOCKED = "SETTINGS_LOCKED"
private const val SHOW_TIME = "SHOW_TIME"
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
private const val CLICK_APP_USAGE = "CLICK_APP_USAGE"
private const val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_APPS_DISPLAYED = "HIDDEN_APPS_DISPLAYED"
private const val SEARCH_ENGINE = "SEARCH_ENGINE"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"
private const val APP_USAGE_STATS = "APP_USAGE_STATS"
private const val APP_OPACITY = "APP_OPACITY"
private const val APP_DARK_COLORS = "APP_DARK_COLORS"
private const val APP_LIGHT_COLORS = "APP_LIGHT_COLORS"
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
private const val CLICK_DATE = "CLICK_DATE"
private const val DOUBLE_TAP = "DOUBLE_TAP"
private const val CUSTOM_FONT = "CUSTOM_FONT"
private const val ALL_APPS_TEXT = "ALL_APPS_TEXT"

private const val TEXT_SIZE_LAUNCHER = "TEXT_SIZE_LAUNCHER"
private const val CLOCK_SIZE_TEXT = "CLOCK_SIZE_TEXT"
private const val TEXT_SIZE_SETTINGS = "TEXT_SIZE_SETTINGS"
private const val TEXT_MARGIN_SIZE = "TEXT_MARGIN_SIZE"

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
                is Int -> editor.putInt(key, value)
                is Double -> editor.putInt(key, value.toInt()) // we store everything as int
                is Float -> editor.putInt(key, value.toInt())
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

    var homePagerOn: Boolean
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

    var opacityNum: Int
        get() = prefs.getInt(APP_OPACITY, 128)
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
                TIME_ALIGNMENT,
                Gravity.Left.name
            ).toString()
            return Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(TIME_ALIGNMENT, value.toString()).apply()

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

    var showTime: Boolean
        get() = prefs.getBoolean(SHOW_TIME, true)
        set(value) = prefs.edit().putBoolean(SHOW_TIME, value).apply()

    var showDate: Boolean
        get() = prefs.getBoolean(SHOW_DATE, true)
        set(value) = prefs.edit().putBoolean(SHOW_DATE, value).apply()

    var showBattery: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY, true)
        set(value) = prefs.edit().putBoolean(SHOW_BATTERY, value).apply()

    var showBatteryIcon: Boolean
        get() = prefs.getBoolean(SHOW_BATTERY_ICON, true)
        set(value) = prefs.edit().putBoolean(SHOW_BATTERY_ICON, value).apply()

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit().putBoolean(HOME_LOCKED, value).apply()

    var settingsLocked: Boolean
        get() = prefs.getBoolean(SETTINGS_LOCKED, false)
        set(value) = prefs.edit().putBoolean(SETTINGS_LOCKED, value).apply()

    var useCustomIconFont: Boolean
        get() = prefs.getBoolean(CUSTOM_FONT, false)
        set(value) = prefs.edit().putBoolean(CUSTOM_FONT, value).apply()

    var useAllAppsText: Boolean
        get() = prefs.getBoolean(ALL_APPS_TEXT, true)
        set(value) = prefs.edit().putBoolean(ALL_APPS_TEXT, value).apply()

    var followAccentColors: Boolean
        get() = prefs.getBoolean(HOME_FOLLOW_ACCENT, false)
        set(value) = prefs.edit().putBoolean(HOME_FOLLOW_ACCENT, value).apply()

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
        get() = loadAction(CLICK_APP_USAGE, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_APP_USAGE, value)

    var clickDateAction: Constants.Action
        get() = loadAction(CLICK_DATE_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_DATE_ACTION, value)

    var doubleTapAction: Constants.Action
        get() = loadAction(DOUBLE_TAP_ACTION, Constants.Action.LockScreen)
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

    var appDarkColors: Constants.DarkColors
        get() {
            return try {
                Constants.DarkColors.valueOf(
                    prefs.getString(APP_DARK_COLORS, Constants.DarkColors.System.name).toString()
                )
            } catch (_: Exception) {
                Constants.DarkColors.System
            }
        }
        set(value) = prefs.edit().putString(APP_DARK_COLORS, value.name).apply()

    var appLightColors: Constants.LightColors
        get() {
            return try {
                Constants.LightColors.valueOf(
                    prefs.getString(APP_LIGHT_COLORS, Constants.LightColors.System.name).toString()
                )
            } catch (_: Exception) {
                Constants.LightColors.System
            }
        }
        set(value) = prefs.edit().putString(APP_LIGHT_COLORS, value.name).apply()

    var language: Constants.Language
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

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    var hiddenAppsDisplayed: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_DISPLAYED, false)
        set(value) = prefs.edit().putBoolean(HIDDEN_APPS_DISPLAYED, value).apply()

    fun getHomeAppModel(i: Int): AppModel {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appModel: AppModel) {
        storeApp("$i", appModel)
    }

    fun setHomeAppName(i: Int, name: String) {
        val nameId = "${APP_NAME}_$i"
        prefs.edit().putString(nameId, name).apply()
    }

    var appShortSwipeUp: AppModel
        get() = loadApp(SHORT_SWIPE_UP)
        set(appModel) = storeApp(SHORT_SWIPE_UP, appModel)
    var appShortSwipeDown: AppModel
        get() = loadApp(SHORT_SWIPE_DOWN)
        set(appModel) = storeApp(SHORT_SWIPE_DOWN, appModel)
    var appShortSwipeLeft: AppModel
        get() = loadApp(SHORT_SWIPE_LEFT)
        set(appModel) = storeApp(SHORT_SWIPE_LEFT, appModel)
    var appShortSwipeRight: AppModel
        get() = loadApp(SHORT_SWIPE_RIGHT)
        set(appModel) = storeApp(SHORT_SWIPE_RIGHT, appModel)

    var appLongSwipeUp: AppModel
        get() = loadApp(LONG_SWIPE_UP)
        set(appModel) = storeApp(LONG_SWIPE_UP, appModel)
    var appLongSwipeDown: AppModel
        get() = loadApp(LONG_SWIPE_DOWN)
        set(appModel) = storeApp(LONG_SWIPE_DOWN, appModel)
    var appLongSwipeLeft: AppModel
        get() = loadApp(LONG_SWIPE_LEFT)
        set(appModel) = storeApp(LONG_SWIPE_LEFT, appModel)
    var appLongSwipeRight: AppModel
        get() = loadApp(LONG_SWIPE_RIGHT)
        set(appModel) = storeApp(LONG_SWIPE_RIGHT, appModel)

    var appClickClock: AppModel
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickUsage: AppModel
        get() = loadApp(CLICK_USAGE)
        set(appModel) = storeApp(CLICK_USAGE, appModel)

    var appClickDate: AppModel
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)

    var appDoubleTap: AppModel
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)


    private fun loadApp(id: String): AppModel {
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

        return AppModel(
            appLabel = appName,
            appPackage = appPackage,
            appAlias = appAlias,
            appActivityName = appActivityName,
            user = userHandle,
            key = null,
        )
    }

    private fun storeApp(id: String, appModel: AppModel) {
        val edit = prefs.edit()
        val appAlias = appModel.appAlias.ifEmpty {
            appModel.appLabel
        }

        edit.putString("${APP_NAME}_$id", appAlias)
        edit.putString("${APP_PACKAGE}_$id", appModel.appPackage)
        edit.putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
        edit.putString("${APP_ALIAS}_$id", appModel.appAlias)
        edit.putString("${APP_USER}_$id", appModel.user.toString())
        edit.apply()
    }

    var textSizeLauncher: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_LAUNCHER, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE_LAUNCHER, value).apply()

    var clockSize: Int
        get() {
            return try {
                prefs.getInt(CLOCK_SIZE_TEXT, 42)
            } catch (_: Exception) {
                42
            }
        }
        set(value) = prefs.edit().putInt(CLOCK_SIZE_TEXT, value).apply()

    var textSizeSettings: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_SETTINGS, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE_SETTINGS, value).apply()

    var textMarginSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_MARGIN_SIZE, 10)
            } catch (_: Exception) {
                10
            }
        }
        set(value) = prefs.edit().putInt(TEXT_MARGIN_SIZE, value).apply()


    // return app label
    fun getAppName(location: Int): String {
        return getHomeAppModel(location).appLabel
    }

    fun getAppAlias(appName: String): String {
        return prefs.getString(appName, "").toString()
    }

    fun setAppAlias(appPackage: String, appAlias: String) {
        prefs.edit().putString(appPackage, appAlias).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
