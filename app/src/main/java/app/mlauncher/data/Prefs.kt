package app.mlauncher.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import app.mlauncher.helper.getUserHandleFromString

private const val APP_LANGUAGE = "app_language"
private const val PREFS_FILENAME = "app.mLauncher"

private const val FIRST_OPEN = "FIRST_OPEN"
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val LOCK_MODE = "LOCK_MODE"
private const val HOME_APPS_NUM = "HOME_APPS_NUM"
private const val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val HOME_ALIGNMENT_BOTTOM = "HOME_ALIGNMENT_BOTTOM"
private const val HOME_CLICK_AREA = "HOME_CLICK_AREA"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val TIME_ALIGNMENT = "TIME_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_DATE = "SHOW_DATE"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SHOW_TIME = "SHOW_TIME"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
private const val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
private const val SHOW_HINT_COUNTER = "SHOW_HINT_COUNTER"
private const val APP_THEME = "APP_THEME"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"

private const val SWIPE_RIGHT = "SWIPE_RIGHT"
private const val SWIPE_LEFT = "SWIPE_LEFT"
private const val SWIPE_DOWN = "SWIPE_DOWN"
private const val SWIPE_UP = "SWIPE_UP"
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val CLICK_DATE = "CLICK_DATE"
private const val DOUBLE_TAP = "DOUBLE_TAP"

private const val TEXT_SIZE = "text_size"

class Prefs(val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_OPEN, value).apply()

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit().putBoolean(FIRST_SETTINGS_OPEN, value).apply()


    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit().putBoolean(LOCK_MODE, value).apply()

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit().putBoolean(AUTO_SHOW_KEYBOARD, value).apply()

    var homeAppsNum: Int
        get() {
            return try {
                prefs.getInt(HOME_APPS_NUM, 4)
            } catch (_: Exception) {
                4
            }
        }
        set(value) = prefs.edit().putInt(HOME_APPS_NUM, value).apply()

    var homeAlignment: Constants.Gravity
        get() {
            return try {
                val string = prefs.getString(
                    HOME_ALIGNMENT,
                    Constants.Gravity.Left.name
                ).toString()
                Constants.Gravity.valueOf(string)
            } catch (_: Exception) {
                Constants.Gravity.Left
            }
        }
        set(value) = prefs.edit().putString(HOME_ALIGNMENT, value.toString()).apply()

    var homeAlignmentBottom: Boolean
        get() = prefs.getBoolean(HOME_ALIGNMENT_BOTTOM, false)
        set(value) = prefs.edit().putBoolean(HOME_ALIGNMENT_BOTTOM, value).apply()

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit().putBoolean(HOME_CLICK_AREA, value).apply()

    var clockAlignment: Constants.Gravity
        get() {
            val string = prefs.getString(
                TIME_ALIGNMENT,
                Constants.Gravity.Left.name
            ).toString()
            return Constants.Gravity.valueOf(string)
        }
        set(value) = prefs.edit().putString(TIME_ALIGNMENT, value.toString()).apply()

    var drawerAlignment: Constants.Gravity
        get() {
            val string = prefs.getString(
                DRAWER_ALIGNMENT,
                Constants.Gravity.Right.name
            ).toString()
            return Constants.Gravity.valueOf(string)
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

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit().putBoolean(HOME_LOCKED, value).apply()

    var swipeLeftAction: Constants.Action
        get() = loadAction(SWIPE_LEFT_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(SWIPE_LEFT_ACTION, value)

    var swipeRightAction: Constants.Action
        get() = loadAction(SWIPE_RIGHT_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(SWIPE_RIGHT_ACTION, value)

    var swipeDownAction: Constants.Action
        get() = loadAction(SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
        set(value) = storeAction(SWIPE_DOWN_ACTION, value)

    var swipeUpAction: Constants.Action
        get() = loadAction(SWIPE_UP_ACTION, Constants.Action.ShowAppList)
        set(value) = storeAction(SWIPE_UP_ACTION, value)

    var clickClockAction: Constants.Action
        get() = loadAction(CLICK_CLOCK_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_CLOCK_ACTION, value)

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
                Constants.Theme.valueOf(prefs.getString(APP_THEME, Constants.Theme.System.name).toString())
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) = prefs.edit().putString(APP_THEME, value.name).apply()

    var language: Constants.Language
        get() {
            return try {
                Constants.Language.valueOf(prefs.getString(APP_LANGUAGE, Constants.Language.English.name).toString())
            } catch (_: Exception) {
                Constants.Language.English
            }
        }
        set(value) = prefs.edit().putString(APP_LANGUAGE, value.name).apply()

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit().putBoolean(HIDDEN_APPS_UPDATED, value).apply()

    var toShowHintCounter: Int
        get() = prefs.getInt(SHOW_HINT_COUNTER, 1)
        set(value) = prefs.edit().putInt(SHOW_HINT_COUNTER, value).apply()

    fun getHomeAppModel(i:Int): AppModel {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appModel: AppModel) {
        storeApp("$i", appModel)
    }

    fun setHomeAppName(i: Int, name: String) {
        val nameId = "${APP_NAME}_$i"
        prefs.edit().putString(nameId, name).apply()
    }

    var appSwipeRight: AppModel
        get() = loadApp(SWIPE_RIGHT)
        set(appModel) = storeApp(SWIPE_RIGHT, appModel)

    var appSwipeLeft: AppModel
        get() = loadApp(SWIPE_LEFT)
        set(appModel) = storeApp(SWIPE_LEFT, appModel)

    var appSwipeDown: AppModel
        get() = loadApp(SWIPE_DOWN)
        set(appModel) = storeApp(SWIPE_DOWN, appModel)

    var appSwipeUp: AppModel
        get() = loadApp(SWIPE_UP)
        set(appModel) = storeApp(SWIPE_UP, appModel)

    var appClickClock: AppModel
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickDate: AppModel
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)

    var appDoubleTap: AppModel
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)


    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "").toString()
        val pack = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val alias = prefs.getString("${APP_ALIAS}_$id", "").toString()
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "").toString()

        val userHandleString = try { prefs.getString("${APP_USER}_$id", "").toString() } catch (_: Exception) { "" }
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)

        return AppModel(
            appLabel = name,
            appPackage = pack,
            appAlias = alias,
            appActivityName = activity,
            user = userHandle,
            key = null,
        )
    }

    private fun storeApp(id: String, appModel: AppModel) {
        val edit = prefs.edit()
        edit.putString("${APP_NAME}_$id", appModel.appLabel)
        edit.putString("${APP_PACKAGE}_$id", appModel.appPackage)
        edit.putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
        edit.putString("${APP_ALIAS}_$id", appModel.appAlias)
        edit.putString("${APP_USER}_$id", appModel.user.toString())
        edit.apply()
    }

    var textSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()


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
}
